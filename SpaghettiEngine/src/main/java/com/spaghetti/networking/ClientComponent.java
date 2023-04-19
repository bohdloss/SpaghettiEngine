package com.spaghetti.networking;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;

import com.spaghetti.core.Game;
import com.spaghetti.world.GameComponent;
import com.spaghetti.world.GameObject;
import com.spaghetti.events.GameEvent;
import com.spaghetti.networking.ConnectionEndpoint.Priority;
import com.spaghetti.networking.events.OnClientBanned;
import com.spaghetti.networking.events.OnClientConnect;
import com.spaghetti.networking.events.OnClientDisconnect;
import com.spaghetti.networking.events.OnConnectionRefused;
import com.spaghetti.networking.events.OnInvalidToken;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

public abstract class ClientComponent extends NetworkComponent {

	// Queue events
	protected ArrayList<NetworkFunction> functions_queue1 = new ArrayList<>(256);
	protected ArrayList<NetworkFunction> functions_queue2 = new ArrayList<>(256);

	// Client data
	protected ConnectionManager manager;
	protected ClientFlags flags;

	// Client variables
	protected boolean giveUp;
	protected int reconnectAttempts = 10;

	public ClientComponent() {
	}

	@Override
	public void initialize(Game game) throws Throwable {
		this.game = game;

		flags = new ClientFlags();
		manager = new ConnectionManager(this);
		reconnectAttempts = this.game.getEngineSetting("network.reconnectAttempts");
	}

	@Override
	public void postInitialize() throws Throwable {
	}

	@Override
	public void preTerminate() throws Throwable {
	}

	@Override
	public void terminate() throws Throwable {
		internal_disconnect();
	}

	@Override
	public void loop(float delta) throws Throwable {
		ConnectionEndpoint endpoint = manager.getEndpoint();

		// Check if we are connected to a server
		if(!isConnected()) {
			return;
		}

		try {
			flags.firstTime = false;

			// Can write
			if (endpoint.canSend() && endpoint.getPriority() != Priority.RECEIVE) {

				// Swap functions queues
				ArrayList<NetworkFunction> first = functions_queue1;
				functions_queue1 = functions_queue2;
				functions_queue2 = first;

				// Write queued special functions
				functions_queue2.forEach(func -> func.execute(manager));

				// Immediately clear list on clients
				functions_queue2.clear();

				// Write data about each object that needs an update
				manager.writeCompleteReplication();

				// Send / receive packets
				endpoint.send();

			} // write

			// Can read
			if (endpoint.canReceive() && endpoint.getPriority() != Priority.SEND) {

				// Read incoming packet
				endpoint.receive();

				// Parse it
				manager.parsePacket();

			} // read
		} catch (Throwable t) {
			internal_clienterror(t); // Something went wrong, attempt reconnection
		}
	}

	// Client interface

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

	protected void _sendMessage(ConnectionEndpoint endpoint, byte type, String message) throws Throwable {
		endpoint.clear();
		endpoint.getWriteBuffer().putByte(type);
		endpoint.getWriteBuffer().putString(message);
		endpoint.waitCanSend();
		endpoint.send();
	}

	protected void internal_clienterror(Throwable t) {
		// If the goodbye flag is on, it means we can ignore errors and perform
		// disconnection
		if (flags.goodbye) {
			internal_disconnect(false);
			return;
		}

		// Socket error, attempt to reconnect
		Logger.error("Exception occurred, attempting reconnection", t);
		String ip = manager.getEndpoint().getRemoteIp();
		int port = manager.getEndpoint().getRemotePort();
		flags.await = true;

		giveUp = false;
		boolean status = false;
		for (int attemtps = 0; attemtps < reconnectAttempts; attemtps++) {
			if(giveUp) {
				break;
			}
			ThreadUtil.sleep(1000);
			if (internal_connect(ip, port, false, flags.clientId)) {
				status = true;
				break;
			}
		}
		if (!status) {
			Logger.warning("Couldn't reconnect with server after " + reconnectAttempts + " attemtps, giving up");
			internal_disconnect();
		}
	}

	public boolean connect(String ip, int port, long token) {
		return (boolean) game.getAuxiliaryDispatcher().quickQueue(() -> internal_connect(ip, port, token));
	}

	protected boolean internal_connect(String ip, int port, long token) {
		return internal_connect(ip, port, true, token);
	}

	protected boolean internal_connect(String ip, int port, boolean disconnectFirst, long token) {
		Logger.error("CONNECTION ATTEMPT BEGIN");
		if (isConnected() && disconnectFirst) {

			// Need to disconnect first
			Logger.warning("Already connected, disconnecting first");
			internal_disconnect();
		}

		try {

			// Establish connection
			ConnectionEndpoint endpoint = internal_connectsocket(ip, port);

			// New handshake logic

			// Send our precious token!
			endpoint.clear();
			endpoint.getWriteBuffer().putByte(TOKEN); // Packet type
			endpoint.getWriteBuffer().putLong(token);
			endpoint.waitCanSend();
			endpoint.send();

			// Receive response from server
			endpoint.clear();
			endpoint.waitCanReceive();
			endpoint.receive();
			NetworkBuffer readBuf = endpoint.getReadBuffer();
			byte packetType = readBuf.getByte();
			String message;
			switch(packetType) {
			case HUG:
				message = readBuf.getString();
				if(flags.await) {
					// We were supposed to receive the RECONNECTED packet
					giveUp = true;
					internal_disconnect(false);
					return false;
				}
				game.getEventDispatcher().raiseEvent(new OnClientConnect(manager, token));
				break;
			case INVALID_TOKEN:
				message = readBuf.getString();
				game.getEventDispatcher().raiseEvent(new OnInvalidToken(token, message));
				giveUp = true;
				internal_disconnect(false);
				return false;
			case BANNED:
				message = readBuf.getString();
				game.getEventDispatcher().raiseEvent(new OnClientBanned(manager, token, message));
				giveUp = true;
				internal_disconnect(false);
				return false;
			case REACHED_MAX:
				message = readBuf.getString();
				game.getEventDispatcher().raiseEvent(new OnConnectionRefused(REACHED_MAX, message));
				giveUp = true;
				internal_disconnect(false);
				return false;
			case SNEAKED_IN:
				message = readBuf.getString();
				game.getEventDispatcher().raiseEvent(new OnConnectionRefused(SNEAKED_IN, message));
				giveUp = true;
				internal_disconnect(false);
				return false;
			case RECONNECTED:
				message = readBuf.getString();
				if(!flags.await) {
					// Expected HUG
					giveUp = true;
					internal_disconnect(false);
					return false;
				}
				flags.await = false;
				break;
			default:
				giveUp = true;
				internal_disconnect(false);
				return false;
			}

			endpoint.setPriority(Priority.RECEIVE);
			flags.goodbye = false;
			flags.firstTime = true;
			flags.clientId = token;
			manager.setEndpoint(endpoint);
		} catch (UnknownHostException e) {
			Logger.warning("Host could not be resolved: " + ip);
			return false;
		} catch (IOException e) {
			Logger.warning("I/O error occurred while connecting: " + e.getClass().getName() + ": " + e.getMessage());
			return false;
		} catch (Throwable t) {
			Logger.error("Unknown error occurred while connecting", t);
			return false;
		}
		Logger.info("Connected to " + ip + " at port " + port);
		return true;
	}

	public boolean disconnect() {
		return (boolean) game.getAuxiliaryDispatcher().quickQueue(this::internal_disconnect);
	}

	protected boolean internal_disconnect() {
		return internal_disconnect(true);
	}

	protected boolean internal_disconnect(boolean sendGoodbye) {
		if (!isConnected()) {
			return true;
		}
		String remoteIp = getRemoteIp();
		int remotePort = getRemotePort();
		ConnectionEndpoint endpoint = manager.getEndpoint();

		// Say goodbye to the server
		flags.firstTime = false;
		flags.goodbye = true;
		if (sendGoodbye && endpoint != null) {
			try {
				endpoint.clear();
				endpoint.getWriteBuffer().putByte(GOODBYE);
				endpoint.waitCanSend();
				endpoint.send();
			} catch(Throwable t) {
			}
			game.getEventDispatcher().raiseEvent(new OnClientDisconnect(manager, flags.clientId));
		}

		// Destroy endpoint
		endpoint.disconnect();
		endpoint.destroy();

		Logger.info("Disconnected from " + remoteIp + " from port " + remotePort);
		return true;
	}

	// Abstract methods
	protected abstract ConnectionEndpoint internal_connectsocket(String ip, int port) throws Throwable;

	// Getters and setters

	public boolean isConnected() {
		if(manager.getEndpoint() == null) {
			return false;
		}
		return manager.getEndpoint().isConnected();
	}

	public String getRemoteIp() {
		if(manager.getEndpoint() == null) {
			return null;
		}
		return manager.getEndpoint().getRemoteIp();
	}

	public int getRemotePort() {
		if(manager.getEndpoint() == null) {
			return 0;
		}
		return manager.getEndpoint().getRemotePort();
	}

	public String getLocalIp() {
		if(manager.getEndpoint() == null) {
			return null;
		}
		return manager.getEndpoint().getLocalIp();
	}

	public int getLocalPort() {
		if(manager.getEndpoint() == null) {
			return 0;
		}
		return manager.getEndpoint().getLocalPort();
	}

	public ConnectionManager getConnection() {
		return manager;
	}

	public int getReconnectAttempts() {
		return reconnectAttempts;
	}

	public void setReconnectAttempts(int reconnectAttempts) {
		this.reconnectAttempts = reconnectAttempts;
	}

}