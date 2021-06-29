package com.spaghetti.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.spaghetti.core.*;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.JoinHandler;
import com.spaghetti.interfaces.NetworkFunction;
import com.spaghetti.networking.NetworkConnection.Priority;
import com.spaghetti.networking.events.OnClientBanned;
import com.spaghetti.networking.events.OnClientConnect;
import com.spaghetti.networking.events.OnClientDisconnect;
import com.spaghetti.networking.events.OnClientUnbanned;
import com.spaghetti.networking.tcp.AsyncServerSocket;
import com.spaghetti.networking.tcp.TCPConnection;
import com.spaghetti.utils.*;

public abstract class ServerCore extends CoreComponent {

	// Queue events
	protected Object queue_lock = new Object();
	protected ArrayList<NetworkFunction> functions = new ArrayList<>(256);

	// Server data
	protected JoinHandler joinHandler;
	protected ConcurrentHashMap<Long, NetworkConnection> clients = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<Long, ClientFlags> flags = new ConcurrentHashMap<>();

	// Server variables
	protected int maxClients = 10;
	protected long awaitReconnect = 10000; // 10 secs
	protected int maxDisconnections = 10;

	public ServerCore() {
		joinHandler = new DefaultJoinHandler();
	}

	@Override
	protected void initialize0() throws Throwable {
		getGame().getEventDispatcher().registerEventHandler((isClient, object, event) -> {
			if (event instanceof OnClientConnect) {
				OnClientConnect occ = (OnClientConnect) event;
				queueWriteObjectTree(occ.getClient().player);
			}
			if (event instanceof OnClientDisconnect) {
				OnClientDisconnect ocd = (OnClientDisconnect) event;
				queueWriteObjectDestruction(ocd.getClient().player);
			}
		});

		internal_bind(9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_banall();
		clients.clear();
		clients = null;
		flags.clear();
		flags = null;

		internal_unbind();
	}

	// Packet read, write loop
	@Override
	protected void loopEvents(float delta) throws Throwable {
		// Catch any exception for safety
		try {
		
			if (getClientsAmount() != 0) {
	
				synchronized (queue_lock) {
					for (Entry<Long, NetworkConnection> entry : clients.entrySet()) {
						try {
							Long clientId = entry.getKey();
							NetworkConnection client = entry.getValue();
							ClientFlags clientFlags = flags.get(clientId);
							if (!client.isConnected()) {
								continue;
							}
							
							// Can send
							if(client.canSend() && client.getPriority() != Priority.RECEIVE) {
								
								// We need to send more data when a client just connected
								if(clientFlags.firstTime) {
									
									clientFlags.firstTime = false;
									
									// Write level structure
									client.writeLevelStructure();
		
									// Write some info about the owned player
									client.writeActivePlayer();
									client.writeActiveCamera();
									client.writeActiveController();
		
									// Flush the packet
									client.setReliable(true);
									client.send();
		
									// Turn on force replication flag
									client.setForceReplication(true);
									client.setReliable(true);
								} else {
									
									// Execute any queued special function
									functions.forEach(func -> func.execute(client));
									functions.clear();
			
									// Write data about every object that needs to be updated
									client.writeObjectReplication();
									client.setForceReplication(false);
			
									// Write to network
									client.send();
								}
							} // send
							
							// Can receive
							if(client.canReceive() && client.getPriority() != Priority.SEND) {
								// Read incoming packets
								client.receive();
								
								// Parse them
								client.parsePacket();
							} // receive
							
						} catch (Throwable t) {
							// Something went wrong, wait for reconnection
							clientError(t, entry.getKey());
						}
					}
				}
	
				// Kick clients that didn't reconnect in time
				for (Iterator<Entry<Long, NetworkConnection>> iterator = clients.entrySet().iterator(); iterator.hasNext();) {
					Entry<Long, NetworkConnection> entry = iterator.next();
					Long clientId = entry.getKey();
					NetworkConnection client = entry.getValue();
					ClientFlags clientFlags = flags.get(clientId);
					
					boolean remove = !client.isConnected()
							&& System.currentTimeMillis() > clientFlags.lostConnectionTime + awaitReconnect
							&& clientFlags.await;
					if (remove) {
						Logger.info("Client " + entry.getKey() + " did not reconnect in time");
						internal_kick(entry.getKey());
						iterator.remove();
					}
				}
	
			}
			
			// Accept new clients
			if (isBound()) {
				internal_accept();
			}
		
		} catch(Throwable t) {
			Logger.error("Server error:", t);
		}
	}

	// Server interface

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getServer();
	}

	public void queueNetworkFunction(NetworkFunction function) {
		synchronized(queue_lock) {
			functions.add(function);
		}
	}
	
	public void queueEvent(GameObject issuer, GameEvent event) {
		if (event == null) {
			throw new IllegalArgumentException();
		}
		synchronized (queue_lock) {
			functions.add(client -> {
				if(!event.skip(client, false)) {
					client.writeGameEvent(issuer, event);
				}
			});
		}
	}

	public void queueIntention(GameObject issuer, long intention) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeIntention(issuer, intention));
		}
	}

	public void queueRPC(RPC rpc) {
		synchronized (queue_lock) {
			functions.add(client -> {
				if(!rpc.skip(client, false)) {
					client.writeRPC(rpc);
				}
			});
		}
	}

	public void queueWriteLevel() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeLevelStructure);
		}
	}

	public void queueWriteData() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeObjectReplication);
		}
	}

	public void queueWriteObjectTree(GameObject obj) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeObjectTree(obj));
		}
	}

	public void queueWriteObjectDestruction(GameObject obj) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeObjectDestruction(obj));
		}
	}

	public void queueWriteComponentDestruction(GameComponent comp) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeComponentDestruction(comp));
		}
	}

	public void queueWriteActiveCamera() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeActiveCamera);
		}
	}

	public void queueWriteActiveController() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeActiveController);
		}
	}

	public void queueWriteActivePlayer() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeActivePlayer);
		}
	}

	public void queueWriteObject(GameObject obj) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeObject(obj));
		}
	}

	public void queueWriteComponent(GameComponent comp) {
		synchronized (queue_lock) {
			functions.add(client -> client.writeComponent(comp));
		}
	}

	// Internal functions

	protected boolean _increaseDisconnections(long clientId) {
		ClientFlags clientFlags = flags.get(clientId);
		if (clientFlags.disconnections == maxDisconnections) {
			Logger.warning("Client " + clientId + " lost connection too many times (" + clientFlags.disconnections
					+ ") and will now be banned from this server");
			internal_ban(clientId);
			return true;
		}
		return false;
	}
	
	protected void clientError(Throwable error, long clientId) {
		Logger.error("Exception occurred in client " + clientId, error);

		if(!_increaseDisconnections(clientId)) {
			NetworkConnection client = clients.get(clientId);
			ClientFlags clientFlags = flags.get(clientId);
			
			clientFlags.await = true;
			client.disconnect();
			clientFlags.lostConnectionTime = System.currentTimeMillis();
			
			Logger.info("Awaiting reconnection for " + awaitReconnect + " ms");
		}
	}

	public boolean kick(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_kick(id));
	}

	protected boolean internal_kick(long id) {
		NetworkConnection worker = clients.remove(id);
		if (worker == null) {
			return false;
		}
		worker.destroy();
		_increaseDisconnections(id);
		getGame().getEventDispatcher().raiseEvent(null, new OnClientDisconnect(worker, id));
		Logger.info("Kicked client " + id);
		return true;
	}

	public boolean ban(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_ban(id));
	}

	protected boolean internal_ban(long id) {
		NetworkConnection worker = clients.get(id);
		if (worker != null) {
			Logger.info("Client " + id + " is still connected, kicking first");
			internal_kick(id);
		}
		ClientFlags clientFlags = flags.get(id);
		if (clientFlags.banned) {
			Logger.warning("Client " + id + " already banned");
		} else {
			clientFlags.banned = true;
			getGame().getEventDispatcher().raiseEvent(null, new OnClientBanned(worker, id));
			Logger.info("Banned client " + id);
		}
		return true;
	}

	public boolean pardon(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_pardon(id));
	}

	protected boolean internal_pardon(long id) {
		ClientFlags clientFlags = flags.get(id);
		if (clientFlags.banned) {
			clientFlags.banned = false;
			getGame().getEventDispatcher().raiseEvent(null, new OnClientUnbanned(clients.get(id), id));
			Logger.info("Unbanned client " + id);
		} else {
			Logger.warning("Client " + id + " was not banned");
		}
		return true;
	}

	public boolean kickAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_kickall);
	}

	protected boolean internal_kickall() {
		boolean last = true;
		for (Long id : clients.keySet()) {
			last &= internal_kick(id);
		}
		return last;
	}

	public boolean banAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_banall);
	}

	protected boolean internal_banall() {
		boolean last = true;
		for (Long id : clients.keySet()) {
			last &= internal_ban(id);
		}
		return last;
	}

	public boolean pardonAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_pardonall);
	}

	protected boolean internal_pardonall() {
		boolean last = true;
		for(Entry<Long, NetworkConnection> entry : clients.entrySet()) {
			last &= internal_pardon(entry.getKey());
		}
		return last;
	}

	protected boolean internal_accept() throws Throwable {
		// Accept connection
		Object socket = internal_acceptsocket();
		if(socket == null) {
			return false;
		}
		
		// Hash ip and port
		long hash = internal_hashsocket(socket);

		// Instantiate a new ClientFlags object and register it
		Long clientId = new Long(hash);
		ClientFlags clientFlags = flags.get(clientId);
		if(clientFlags == null) {
			clientFlags = new ClientFlags();
			flags.put(clientId, clientFlags);
		}
		
		// Check if client is banned
		if (!clientFlags.banned) {

			// Check if it is already instantiated here and add it
			if (!clients.containsKey(clientId)) {
				
				// If we reached the maximum number of clients, refuse connection
				if(getClientsAmount() < maxClients) {
				
					// New connection, initialize it
					NetworkConnection client = internal_initworker();
					client.connect(socket);
					clients.put(clientId, client);
	
					// Perform handshake
					if(!internal_handshake(client)) {
						Logger.warning("Client handshake failed (" + clientId + ")");
						internal_kick(clientId);
					} else {
						joinHandler.handleJoin(false, client);
						getGame().getEventDispatcher().raiseEvent(null, new OnClientConnect(client, clientId));
						clientFlags.firstTime = true;
						Logger.info("ACCEPTED connection from client (" + clientId + ")");
						return true;
					}
				}
			} else {

				// Existing connection, attempt reconnection
				NetworkConnection client = clients.get(clientId);
				if (!client.isConnected()) {
					// Provide new socket to worker
					client.connect(socket);

					// Re-perform handshake for security reasons
					if (!internal_handshake(client)) {
						Logger.warning("Client handshake failed (" + clientId + ")");
						internal_kick(clientId);
					} else {
						// Old connection successfully re-established, return true
						clientFlags.await = false;
						Logger.info("RECONNECTED with client (" + clientId + ")");
						return true;
					}
				} else {
					Logger.warning("Attempt to connect more than once from the same network");
				}
			}
		}

		// Failure, close socket and return false
		Logger.warning("REFUSED connection from client (" + clientId + ")");
		try {
			internal_closesocket(socket);
		} catch (Throwable e) {
		}
		return false;
	}

	// Abstract functions related to internal_accept
	protected abstract Object internal_acceptsocket() throws Throwable;
	protected abstract long internal_hashsocket(Object object);
	protected abstract void internal_closesocket(Object object) throws Throwable;
	protected abstract NetworkConnection internal_initworker();
	
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
		} catch(Throwable t) {
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

	protected boolean internal_handshake(NetworkConnection client) {
		try {
			client.receive();
			client.readAuthentication();
			boolean ret = client.writeAuthentication();
			client.send();
			return ret;
		} catch(Throwable t) {
			Logger.error("Handshake error:", t);
			return false;
		}
	}

	// Getters and setters

	public boolean isConnected(long id) {
		NetworkConnection client;
		return (client = clients.get(id)) == null ? false : client.isConnected();
	}

	public int getClientsAmount() {
		return clients.size();
	}

	public long getClientId(NetworkConnection client) {
		for (Entry<Long, NetworkConnection> entry : clients.entrySet()) {
			if (entry.getValue() == client) {
				return entry.getKey();
			}
		}
		return 0;
	}

	public NetworkConnection getClientById(long id) {
		return clients.get(id);
	}

	public String getRemoteIp(long id) {
		return clients.get(id).getRemoteIp();
	}

	public int getRemotePort(long id) {
		return clients.get(id).getRemotePort();
	}

	public abstract String getLocalIp();
	
	public abstract int getLocalPort();

	public abstract boolean isBound();

	public JoinHandler getJoinHandler() {
		return joinHandler;
	}

	public void setJoinHandler(JoinHandler joinHandler) {
		this.joinHandler = joinHandler;
	}

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