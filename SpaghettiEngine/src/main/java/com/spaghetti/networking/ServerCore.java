package com.spaghetti.networking;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.utils.HashUtil;
import com.spaghetti.world.GameComponent;
import com.spaghetti.world.GameObject;
import com.spaghetti.events.GameEvent;
import com.spaghetti.exceptions.EndpointException;
import com.spaghetti.networking.ConnectionEndpoint.Priority;
import com.spaghetti.networking.events.OnClientBanned;
import com.spaghetti.networking.events.OnClientConnect;
import com.spaghetti.networking.events.OnClientDisconnect;
import com.spaghetti.networking.events.OnClientKicked;
import com.spaghetti.networking.events.OnClientUnbanned;
import com.spaghetti.utils.Logger;

public abstract class ServerCore extends NetworkCore {

	// Queue events
	protected ArrayList<NetworkFunction> functions_queue1 = new ArrayList<>(256);
	protected ArrayList<NetworkFunction> functions_queue2 = new ArrayList<>(256);

	// Clients data
	protected ConcurrentHashMap<Long, ConnectionManager> clients = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<Long, ClientFlags> flags = new ConcurrentHashMap<>();

	// Server variables
	protected int maxClients = 10;
	protected long awaitReconnect = 10000; // 10 secs
	protected int maxDisconnections = 10;

	// Reserved client tokens
	protected Random tokenGen = new Random();
	protected ArrayList<Long> clientTokens = new ArrayList<>();

	public ServerCore() {
	}

	@Override
	protected void initialize0() throws Throwable {
		internal_bind(getGame().getEngineSetting("network.port"));
		maxClients = getGame().getEngineSetting("network.maxClients");
		awaitReconnect = getGame().getEngineSetting("network.awaitTimeout");
		maxDisconnections = getGame().getEngineSetting("network.maxDisconnections");
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_banall("Server closed");
		clients.clear();
		clients = null;
		flags.clear();
		flags = null;

		internal_unbind();
	}

	// Manage client tokens
	public long reserveToken() {
		Long token = tokenGen.nextLong();
		while(clientTokens.contains(token)) {
			token = tokenGen.nextLong();
		}
		clientTokens.add(token);
		return token;
	}

	public void freeToken(Long token) {
		clientTokens.remove(token);
	}

	// Packet read, write loop
	@Override
	protected void loopEvents(float delta) throws Throwable {
		// Catch any exception for extra safety
		try {

			// Swap functions queues
			ArrayList<NetworkFunction> first = functions_queue1;
			functions_queue1 = functions_queue2;
			functions_queue2 = first;

			// Skip if no clients are connected
			if (getClientsAmount() != 0) {

				// Iterate through clients
				for (Entry<Long, ConnectionManager> entry : clients.entrySet()) {

					// One more try catch for each client
					try {
						Long clientId = entry.getKey();
						ClientFlags clientFlags = flags.get(clientId);
						ConnectionManager manager = entry.getValue();
						ConnectionEndpoint endpoint = manager.getEndpoint();

						if (endpoint == null || !endpoint.isConnected()) {
							continue;
						}

						// Can send
						if (endpoint.canSend() && endpoint.getPriority() != Priority.RECEIVE) {
							endpoint.clear();
							endpoint.getWriteBuffer().putByte(DATA);

							// We need to send more data when a client just connected
							if (clientFlags.firstTime) {

								clientFlags.firstTime = false;

								// Write level structure
								manager.writeLevelStructure();

								// Write some info about the owned player
								// TODO update ClientState

								// Flush the packet
								endpoint.setReliable(true);
								endpoint.send();

								// Turn on force replication flag for the next packet
								manager.setForceReplication(true);
								endpoint.setReliable(true);
							} else {

								// Execute any queued special function
								functions_queue2.forEach(func -> func.execute(manager));

								// Write data about every object that needs to be updated
								manager.writeCompleteReplication();
								manager.setForceReplication(false);

								// Write to network
								endpoint.send();
							}
							endpoint.setPriority(Priority.RECEIVE);
						} // send

						// Can receive
						if (endpoint.canReceive() && endpoint.getPriority() != Priority.SEND) {
							endpoint.clear();

							// Read incoming packet
							endpoint.receive();

							// Which kind of packet is it?
							byte packetType = endpoint.getReadBuffer().getByte();
							switch(packetType) {
							case DATA:
								// Parse data
								manager.parsePacket();
								break;
							case GOODBYE:
								// Mark this client as ready to leave
								clientFlags.goodbye = true;
								break;
							case PING:
								// Calculate ping of client
								long sendTime = endpoint.getReadBuffer().getLong();
								long diff = System.currentTimeMillis() - sendTime;
								clientFlags.ping = diff;
								break;
							default:
								internal_kick(clientId, null);
								continue;
							}
							endpoint.setPriority(Priority.SEND);
						} // receive

					} catch (Throwable t) {
						_clientError(t, entry.getKey()); // Something went wrong, wait for reconnection
					} // Client catch

				} // Client loop

				// Kick clients that didn't reconnect in time
				_clientOutdated();

			} // else no clients are connected

			// Empty function queue now
			functions_queue2.clear();

			if (isBound()) {
				internal_accept(); // Accept new clients
			}

		} catch (Throwable t) {
			Logger.error("Uncaught server error:", t);
		} // Emergency catch block
	}

	// Server interface

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getServer();
	}

	@Override
	public void queueNetworkFunction(NetworkFunction function) {
		functions_queue1.add(function);
	}

	@Override
	public void queueEvent(GameEvent event) {
		if (event == null) {
			throw new IllegalArgumentException();
		}

		functions_queue1.add(client -> {
			if (event.needsReplication(client)) {
				client.writeGameEvent(event);
			}
		});
	}

	@Override
	public void queueRPC(RemoteProcedure rpc) {
		if (rpc == null) {
			throw new IllegalArgumentException();
		}

		functions_queue1.add(client -> {
			client.writeRemoteProcedure(rpc);
		});
	}

	@Override
	public void queueWriteLevel() {
		functions_queue1.add(ConnectionManager::writeLevelStructure);
	}

	@Override
	public void queueWriteData() {
		functions_queue1.add(ConnectionManager::writeCompleteReplication);
	}

	@Override
	public void queueWriteObjectFull(GameObject obj) {
		queueWriteObjectTree(obj);
		queueWriteObject(obj);
	}

	@Override
	public void queueWriteObjectTree(GameObject obj) {
		functions_queue1.add(client -> client.writeObjectTree(obj));
	}

	@Override
	public void queueWriteObjectDestruction(GameObject obj) {
		functions_queue1.add(client -> client.writeObjectDestruction(obj));
	}

	@Override
	public void queueWriteComponentDestruction(GameComponent comp) {
		functions_queue1.add(client -> client.writeComponentDestruction(comp));
	}

	@Override
	public void queueWriteObject(GameObject obj) {
		functions_queue1.add(client -> client.writeObjectReplication(obj));
	}

	@Override
	public void queueWriteComponent(GameComponent comp) {
		functions_queue1.add(client -> client.writeComponentReplication(comp));
	}

	// Internal functions

	protected ClientFlags _getClientFlags(long clientId) {
		ClientFlags clientFlags = flags.get(clientId);
		if (clientFlags == null) {
			clientFlags = new ClientFlags();
			flags.put(clientId, clientFlags);
		}
		return clientFlags;
	}

	protected ConnectionManager _getConnectionManager(long clientId) {
		ConnectionManager manager = clients.get(clientId);
		if(manager == null) {
			manager = new ConnectionManager(this);
			clients.put(clientId, manager);
		}
		return manager;
	}

	protected boolean _increaseDisconnections(long clientId) {
		ClientFlags clientFlags = flags.get(clientId);
		if (clientFlags.disconnections >= maxDisconnections && maxDisconnections >= 0) {
			Logger.warning("Client " + clientId + " lost connection too many times (" + clientFlags.disconnections
					+ ") and will now be banned from this server");
			internal_ban(clientId, "Too many disconnections");
			return true;
		}
		return false;
	}

	protected void _sendMessage(ConnectionEndpoint endpoint, byte type, String message) {
		endpoint.clear();
		endpoint.getWriteBuffer().putByte(type);
		endpoint.getWriteBuffer().putString(message);
		endpoint.waitCanSend();
		endpoint.send();
	}

	protected void _clientError(Throwable error, long clientId) {
		ClientFlags clientFlags = flags.get(clientId);
		ConnectionManager manager = clients.get(clientId);
		ConnectionEndpoint endpoint = manager.getEndpoint();

		// If the goodbye flag is set, ignore errors and kick
		if (clientFlags.goodbye) {
			internal_kick(clientId, null);
			return;
		}

		Logger.error("Exception occurred in client " + clientId, error);

		if (!_increaseDisconnections(clientId)) {

			_closeEndpoint(endpoint);
			clientFlags.await = true;
			clientFlags.lostConnectionTime = System.currentTimeMillis();

			Logger.info("Awaiting reconnection for " + awaitReconnect + " ms");
		}
	}

	protected void _clientOutdated() {
		for (Iterator<Entry<Long, ConnectionManager>> iterator = clients.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<Long, ConnectionManager> entry = iterator.next();
			Long clientId = entry.getKey();
			ClientFlags clientFlags = flags.get(clientId);
			ConnectionManager manager = entry.getValue();
			ConnectionEndpoint endpoint = manager.getEndpoint();

			boolean remove = !endpoint.isConnected()
					&& System.currentTimeMillis() > clientFlags.lostConnectionTime + awaitReconnect
					&& clientFlags.await;
			if (remove) {
				Logger.info("Client " + clientId + " did not reconnect in time");
				internal_kick(clientId, null);
				iterator.remove();
			}
		}
	}

	protected void _closeEndpoint(ConnectionEndpoint endpoint) {
		endpoint.disconnect();
		endpoint.destroy();
	}

	public boolean kick(long id, String reason) {
		if(reason == null) {
			throw new IllegalArgumentException("Reason cannot be null");
		}
		return (boolean) getDispatcher().quickQueue(() -> internal_kick(id, reason));
	}

	protected boolean internal_kick(long id, String reason) {
		ConnectionManager manager = clients.remove(id);
		if (manager == null) {
			Logger.warning("Cannot kick client " + id + " because it's not recognized");
			return false;
		}
		ConnectionEndpoint endpoint = manager.getEndpoint();

		if (reason != null && endpoint != null) {
			try {
				_sendMessage(endpoint, KICKED, reason);
			} catch (EndpointException e) {
				Logger.error("Error sending goodbye message", e);
			}
		}
		if(endpoint != null) {
			endpoint.disconnect();
			endpoint.destroy();
		}
		_increaseDisconnections(id);

		// If reason is null, we are just updating the internal state of the server
		if(reason == null) {
			getGame().getEventDispatcher().raiseEvent(new OnClientDisconnect(manager, id));
		} else { // Otherwise we are explicitly kicking the client
			getGame().getEventDispatcher().raiseEvent(new OnClientKicked(manager, id, reason));
		}

		manager.setEndpoint(null);
		manager.destroy();
		Logger.info("Kicked client " + id + " for: " + reason);
		return true;
	}

	public boolean ban(long id, String reason) {
		return (boolean) getDispatcher().quickQueue(() -> internal_ban(id, reason));
	}

	protected boolean internal_ban(long id, String reason) {
		ConnectionManager manager = clients.get(id);
		if (manager != null) {
			Logger.info("Client " + id + " is still connected, kicking first");
			internal_kick(id, "You have been banned. Reason: " + reason);
		}
		ClientFlags clientFlags = flags.get(id);
		if (clientFlags.banned) {
			clientFlags.banReason = reason;
			Logger.warning("Client " + id + "'s ban reason has been updated to: " + clientFlags.banReason);
		} else {
			clientFlags.banned = true;
			clientFlags.banReason = reason;
			getGame().getEventDispatcher().raiseEvent(new OnClientBanned(manager, id, reason));
			Logger.info("Banned client " + id + " for: " + reason);
		}
		return true;
	}

	public boolean pardon(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_pardon(id));
	}

	protected boolean internal_pardon(long id) {
		ClientFlags clientFlags = flags.get(id);
		ConnectionManager manager = clients.get(id);
		if (clientFlags.banned) {
			clientFlags.banned = false;
			getGame().getEventDispatcher().raiseEvent(new OnClientUnbanned(manager, id));
			Logger.info("Unbanned client " + id);
		} else {
			Logger.warning("Client " + id + " was not banned");
		}
		return true;
	}

	public boolean kickAll(String reason) {
		return (boolean) getDispatcher().quickQueue(() -> internal_kickall(reason));
	}

	protected boolean internal_kickall(String reason) {
		boolean last = true;
		for (Long id : clients.keySet()) {
			last &= internal_kick(id, reason);
		}
		return last;
	}

	public boolean banAll(String reason) {
		return (boolean) getDispatcher().quickQueue(() -> internal_banall(reason));
	}

	protected boolean internal_banall(String reason) {
		boolean last = true;
		for (Long id : clients.keySet()) {
			last &= internal_ban(id, reason);
		}
		return last;
	}

	public boolean pardonAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_pardonall);
	}

	protected boolean internal_pardonall() {
		boolean last = true;
		for (Long id : clients.keySet()) {
			last &= internal_pardon(id);
		}
		return last;
	}

	protected boolean internal_accept() throws Throwable {
		// Accept connection
		ConnectionEndpoint endpoint = internal_acceptsocket();
		if (endpoint == null) {
			return false;
		}

		// New handshake logic

		// Receive token
		endpoint.clear();
		endpoint.waitCanReceive();
		endpoint.receive();
		byte packetType = endpoint.getReadBuffer().getByte();
		if(packetType != TOKEN) {
			// ???
			_closeEndpoint(endpoint);
			return false;
		}
		long token = endpoint.getReadBuffer().getLong();

		// Verify client token
		if(getGame().<Boolean>getEngineSetting("network.verifyToken")) {

			// Verify
			if(clientTokens.contains(token)) {

				// Valid token
				freeToken(token);
			} else {

				// Invalid token
				_sendMessage(endpoint, INVALID_TOKEN, "You're not supposed to be here!");
				Logger.warning("REFUSED connection from client because it provided and invalid token (???" + token + "???)");

				_closeEndpoint(endpoint);
				return false;
			}
		} else {
			token = HashUtil.longHash(endpoint.getRemoteIp() + ":" + endpoint.getRemotePort());
		}

		// Obtain a client flags object
		ClientFlags clientFlags = _getClientFlags(token);

		// Always flag connection as first time because:
		// Connect: needs to know everything / Reconnect: may have missed something
		clientFlags.firstTime = true;

		// Check if client is banned
		if (!clientFlags.banned) {

			// Check if the client is attempting to reconnect
			if (!clients.containsKey(token)) {

				// If we reached the maximum number of clients, refuse connection
				if (getClientsAmount() < maxClients) {

					// New connection, initialize the connection manager
					endpoint.setPriority(Priority.SEND);
					ConnectionManager manager = _getConnectionManager(token);
					manager.setEndpoint(endpoint);
					clientFlags.clientId = token;

					// Client connect event
					_sendMessage(endpoint, HUG, "Welcome aboard!");
					getGame().getEventDispatcher().raiseEvent(new OnClientConnect(manager, token));
					Logger.info("ACCEPTED connection from client (" + token + ")");

					return true;
				} else {

					// Notify client there are too many players
					_sendMessage(endpoint, REACHED_MAX, "There's only place for " + maxClients + " in this town! *BANG*");
					Logger.warning("REFUSED connection from client because client limit was reached (" + token + ")");

					_closeEndpoint(endpoint);
					return false;
				}
			} else {

				// Existing client, new endpoint, synchronize with manager
				ConnectionManager manager = _getConnectionManager(token);

				// Accept only if we were waiting for this reconnect
				if (clientFlags.await) {

					// Update endpoint
					endpoint.setPriority(Priority.SEND);
					manager.setEndpoint(endpoint);
					clientFlags.await = false;

					// Client endpoint successfully updated, yay!
					_sendMessage(endpoint, RECONNECTED, "Welcome back!");
					Logger.info("RECONNECTED with client (" + token + ")");

					return true;
				} else {

					// Mock the client for trying to perform an illegal operation
					_sendMessage(endpoint, SNEAKED_IN, "Are you insane? Stop attacking, immediately!");
					Logger.warning("REFUSED connection from client who tried a fake reconnect request (" + token + ")");

					_closeEndpoint(endpoint);
					return false;
				}
			}
		} else {

			// Write reason for ban
			_sendMessage(endpoint, BANNED, clientFlags.banReason);
			Logger.warning("REFUSED connection from client because it is banned (" + token + ")");

			_closeEndpoint(endpoint);
			return false;
		}
	}

	// Abstract functions related to internal_accept
	protected abstract ConnectionEndpoint internal_acceptsocket() throws Throwable;

	public boolean bind(int port) {
		return (boolean) getDispatcher().quickQueue(() -> internal_bind(port));
	}

	protected boolean internal_bind(int port) {
		if (isBound()) {
			Logger.warning("Server is already bound, stopping first...");
			internal_unbind();
		}
		try {
			internal_startserver(port);
		} catch (IOException e) {
			Logger.error("I/O exception occurred while starting server on port " + port + ", aborting", e);
			return false;
		} catch (Throwable t) {
			Logger.error("Unknown error occurred while starting server on port " + port + ", aborting", t);
			return false;
		}
		Logger.info("Server now listening on port " + port);
		return true;
	}

	protected abstract void internal_startserver(int port) throws Throwable;

	public boolean unbind() {
		return (boolean) getDispatcher().quickQueue(this::internal_unbind);
	}

	protected boolean internal_unbind() {
		if (!isBound()) {
			Logger.warning("Server is not bound, no need to unbind");
			return false;
		}
		try {
			internal_stopserver();
		} catch (IOException e) {
			Logger.error("I/O error occurred while closing server, ignoring");
		} catch (Throwable t) {
			Logger.error("Unknown error occurred while closing server", t);
		}
		Logger.info("Server unbound");
		return true;
	}

	protected abstract void internal_stopserver() throws Throwable;

	// Getters and setters

	public boolean isConnected(long id) {
		ConnectionManager manager = clients.get(id);
		if(manager == null || manager.getEndpoint() == null) {
			return false;
		}
		return manager.getEndpoint().isConnected();
	}

	public int getClientsAmount() {
		return clients.size();
	}

	public ConnectionManager getClientById(long id) {
		return clients.get(id);
	}

	public String getRemoteIp(long id) {
		ConnectionManager manager = clients.get(id);
		if(manager == null || manager.getEndpoint() == null) {
			return null;
		}
		return manager.getEndpoint().getRemoteIp();
	}

	public int getRemotePort(long id) {
		ConnectionManager manager = clients.get(id);
		if(manager == null || manager.getEndpoint() == null) {
			return 0;
		}
		return manager.getEndpoint().getLocalPort();
	}

	public abstract String getLocalIp();

	public abstract int getLocalPort();

	public abstract boolean isBound();

	public int getMaxClients() {
		return maxClients;
	}

	public void setMaxClients(int maxClients) {
		this.maxClients = maxClients;
	}

	public long getAwaitReconnectTime() {
		return awaitReconnect;
	}

	public void setAwaitReconnectTime(long awaitReconnect) {
		this.awaitReconnect = awaitReconnect;
	}

}