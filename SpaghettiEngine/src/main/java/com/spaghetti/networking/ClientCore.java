package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import com.spaghetti.core.*;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.events.OnClientConnect;
import com.spaghetti.networking.events.OnClientDisconnect;
import com.spaghetti.utils.*;

public abstract class ClientCore extends CoreComponent {

	// Queue events
	protected Object queue_lock = new Object();
	protected ArrayList<NetworkFunction> functions = new ArrayList<>(256);

	// Client data
	protected NetworkConnection worker;
	protected JoinHandler joinHandler;
	protected ClientFlags flags;

	// Client variables
	protected int reconnectAttempts = 10;

	public ClientCore() {
		joinHandler = new DefaultJoinHandler();
		flags = new ClientFlags();
	}

	@Override
	protected void initialize0() throws Throwable {
	}

	@Override
	protected void postInitialize() throws Throwable {
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
					
					flags.firstTime = false;
					
					// Can write
					if(worker.canSend()) {
						
						// Write queued special functions
						functions.forEach(func -> func.execute(worker));
						functions.clear();
	
						// Write data about each object that needs an update
						worker.writeObjectReplication();
	
						// Send / receive packets
						worker.send();
						
					} // write
					
					// Can read
					if(worker.canReceive()) {
						
						// Read incoming packet
						worker.receive();
						
						// Parse it
						worker.parsePacket();
						
					} // read
					
				} catch (Throwable t) {
					internal_clienterror(t, worker, flags.clientId);
				}
			}
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getClient();
	}

	// Client interface

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
				if(!event.skip(client, true)) {
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
				if(!rpc.skip(client, true)) {
					client.writeRPC(rpc);
				}
			});
		}
	}

	public void queueWriteData() {
		synchronized (queue_lock) {
			functions.add(NetworkConnection::writeObjectReplication);
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

	protected void internal_clienterror(Throwable t, NetworkConnection client, long clientId) {
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
			internal_disconnect();
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
			worker.connect(ip, port); // Perform connection

			// Server handshake
			if (!internal_handshake(worker)) {
				Logger.warning("Server handshake failed");
				internal_disconnect();
				return false;
			} else {
				joinHandler.handleJoin(true, worker); // Handle joining server
				flags.firstTime = true;
				getGame().getEventDispatcher().raiseEvent(null, new OnClientConnect(worker, flags.clientId));
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
		flags.firstTime = false;
		worker.disconnect(); // Reset worker socket
		// Detach and destroy level
		Level activeLvl = getGame().getActiveLevel();
		if(activeLvl != null) {
			activeLvl.destroy();
			getGame().detachLevel();
		}
		// Dispatch disconnect event
		getGame().getEventDispatcher().raiseEvent(null, new OnClientDisconnect(worker, flags.clientId));
		Logger.info("Disconnected from " + remoteIp + " from port " + remotePort);
		return true;
	}

	protected boolean internal_handshake(NetworkConnection client) {
		try {
			worker.writeAuthentication();
			worker.send();
			worker.receive();
			return worker.readAuthentication();
		} catch(Throwable t) {
			Logger.error("Handshake error:", t);
			return false;
		}
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

	public NetworkConnection getWorker() {
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