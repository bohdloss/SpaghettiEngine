package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import com.spaghetti.core.*;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.utils.*;

public class Client extends CoreComponent {

	// Queue events
	protected Object queue_lock = new Object();
	protected HashMap<GameObject, Object> events = new HashMap<>();
	protected ArrayList<RPC> rpcs = new ArrayList<>(256);
	protected ArrayList<NetworkFunction> functions = new ArrayList<>(256);

	// Client data
	protected NetworkWorker worker;
	protected JoinHandler joinHandler;
	protected long clientId;

	// Client variables
	protected int reconnectAttempts = 10;

	public Client() {
		joinHandler = new DefaultJoinHandler();
	}

	@Override
	protected void initialize0() throws Throwable {
		worker = new TCPWorker(this);
	}

	@Override
	protected void postInitialize() throws Throwable {
		internal_connect("localhost", 9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_disconnect();
		worker.destroy();
	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		if (isConnected()) {
			synchronized (queue_lock) {
				try {
					// Write events in queue
					events.forEach((issuer, event) -> {
						if (GameEvent.class.isAssignableFrom(event.getClass())) {
							GameEvent game_event = (GameEvent) event;
							if (!game_event.skip(worker, true)) {
								worker.writeGameEvent(issuer, game_event);
							}
						} else {
							worker.writeIntention(issuer, (Long) event);
						}
					});

					// Write remote procedure calls
					rpcs.forEach(rpc -> {
						if (!rpc.skip(worker, true)) {
							worker.writeRPC(rpc);
						}
					});

					// Write queued special functions
					functions.forEach(func -> func.execute(worker));

					// Write data about each object that needs an update
					worker.writeData();

					// Send / receive packets
					worker.writeSocket();
					worker.readSocket();

					// Read incoming packets
					worker.parseOperations();
				} catch (Throwable t) {
					internal_clienterror(t, worker, clientId);
				}
				events.clear();
				rpcs.clear();
				functions.clear();
			}
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getClient();
	}

	// Client interface

	public void queueEvent(GameObject issuer, GameEvent event) {
		if (event == null) {
			throw new IllegalArgumentException();
		}
		synchronized (queue_lock) {
			events.put(issuer, event);
		}
	}

	public void queueIntention(GameObject issuer, long intention) {
		synchronized (queue_lock) {
			events.put(issuer, intention);
		}
	}

	public void queueRPC(RPC rpc) {
		synchronized (queue_lock) {
			rpcs.add(rpc);
		}
	}

	public void queueWriteData() {
		synchronized (queue_lock) {
			functions.add(NetworkWorker::writeData);
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

	protected void internal_clienterror(Throwable t, NetworkWorker client, long clientId) {
		// Socket error, just reconnect
		Logger.error("Exception occurred, attempting reconnection", t);
		String ip = client.getRemoteIp();
		int port = client.getRemotePort();
		boolean status = false;
		for (int attemtps = 0; attemtps < reconnectAttempts; attemtps++) {
			Utils.sleep(1000);
			if (internal_connect(ip, port)) {
				status = true;
				break;
			}
		}
		if (!status) {
			Logger.warning("Couldn't reconnect with server after " + reconnectAttempts + " attemtps, giving up");
		}
	}

	public boolean connect(String ip, int port) {
		return (boolean) getDispatcher().quickQueue(() -> internal_connect(ip, port));
	}

	protected boolean internal_connect(String ip, int port) {
		if (isConnected()) {
			Logger.warning("Already connected, disconnecting first");
			internal_disconnect();
		}
		try {
			Socket socket = new Socket(ip, port); // Perform connection
			joinHandler.handleJoin(true, worker); // Handle joining server
			worker.provideSocket(socket); // Give socket to worker

			// Server handshake
			if (!internal_handshake(worker)) {
				Logger.warning("Server handshake failed");
				internal_disconnect();
				return false;
			} else {
				try {
					// Don't write anything, just read incoming packet
					worker.writeSocket();
					worker.readSocket();

					// We are reading the level structure
					worker.parseOperations();

					// Turn on replication flag
					worker.setForceReplication(true);
					Logger.info("Connection correctly established");
				} catch (Throwable t) {
					internal_clienterror(t, worker, clientId);
				}
				getGame().getEventDispatcher().raiseEvent(null, new OnClientConnect(worker, clientId));
			}
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
		return (boolean) getDispatcher().quickQueue(this::internal_disconnect);
	}

	protected boolean internal_disconnect() {
		if (!isConnected()) {
			return true;
		}
		String remoteIp = getRemoteIp();
		int remotePort = getRemotePort();
		worker.resetSocket(); // Reset worker socket
		getGame().getEventDispatcher().raiseEvent(null, new OnClientDisconnect(worker, clientId));
		Logger.info("Disconnected from " + remoteIp + " from port " + remotePort);
		return true;
	}

	protected boolean internal_handshake(NetworkWorker client) throws Throwable {
		worker.writeAuthentication();
		worker.writeSocket();
		worker.readSocket();
		return worker.readAuthentication();
	}

	// Getters and setters

	public boolean isConnected() {
		return worker.isConnected();
	}

	public String getRemoteIp() {
		return worker.getRemoteIp();
	}

	public int getRemotePort() {
		return worker.getRemotePort();
	}

	public String getLocalIp() {
		return worker.getLocalIp();
	}

	public int getLocalPort() {
		return worker.getLocalPort();
	}

	public NetworkWorker getWorker() {
		return worker;
	}

	public JoinHandler getJoinHandler() {
		return joinHandler;
	}

	public void setJoinHandler(JoinHandler joinHandler) {
		this.joinHandler = joinHandler;
	}

	public int getReconnectAttempts() {
		return reconnectAttempts;
	}

	public void setReconnectAttempts(int reconnectAttemtps) {
		this.reconnectAttempts = reconnectAttemtps;
	}

}