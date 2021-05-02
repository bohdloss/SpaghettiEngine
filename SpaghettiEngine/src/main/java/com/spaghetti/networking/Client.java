package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import com.spaghetti.core.*;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.utils.*;

public class Client extends CoreComponent {

	protected HashMap<GameObject, Object> events = new HashMap<>();
	protected NetworkWorker worker;
	protected JoinHandler joinHandler;
	protected long clientId;
	protected int reconnectAttempts = 10;

	public Client() {
		joinHandler = new DefaultJoinHandler();
	}

	@Override
	protected void initialize0() throws Throwable {
		getGame().getEventDispatcher().registerEventHandler((isClient, issuer, event) -> {
			if (event instanceof OnClientConnect) {
				OnClientConnect occ = (OnClientConnect) event;
				NetworkWorker client = occ.getClient();
				long clientId = occ.getClientId();
				try {
					// Don't write anything, just read incoming packet
					client.writeSocket();
					client.readSocket();

					// We are reading the level structure
					client.parseOperations();

					// Turn on replication flag
					client.setForceReplication(true);
					Logger.info("Connection correctly established");
				} catch (Throwable t) {
					internal_clienterror(t, client, clientId);
				}
			}
		});

		worker = new NetworkWorker(this);
		internal_connect("localhost", 9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_disconnect();
		worker.destroy();
	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		Utils.sleep(25);
		if (isConnected()) {
			try {
				// Write / read routine, in this order for server synchronization
				synchronized (events) {
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
				}
				// Write data about each object that needs an update
				worker.writeData();
				worker.setForceReplication(false);

				// Send packet
				worker.writeSocket();
				worker.readSocket();

				// Read incoming packets
				worker.parseOperations();
			} catch (Throwable t) {
				internal_clienterror(t, worker, clientId);
			}
			synchronized (events) {
				events.clear();
			}
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getClient();
	}

	public void queueEvent(GameObject issuer, GameEvent event) {
		if (event == null) {
			throw new IllegalArgumentException();
		}
		synchronized (events) {
			events.put(issuer, event);
		}
	}

	public void queueIntention(GameObject issuer, long intention) {
		synchronized (events) {
			events.put(issuer, intention);
		}
	}

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
				getGame().getEventDispatcher().raiseEvent(null, new OnClientConnect(worker, clientId));
			}
		} catch (UnknownHostException e) {
			Logger.warning("Host could not be resolved: " + ip);
			return false;
		} catch (IOException e) {
			Logger.warning("I/O error occurred while connecting: " + e.getClass().getName() + ": " + e.getMessage());
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
		Logger.info("Disconnected from " + remoteIp + " from port " + remotePort);
		return true;
	}

	protected boolean internal_handshake(NetworkWorker client) throws IOException {
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