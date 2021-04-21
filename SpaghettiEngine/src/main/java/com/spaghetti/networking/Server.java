package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.JoinHandler;
import com.spaghetti.utils.*;

public class Server extends CoreComponent {

	protected ServerSocket server;
	protected JoinHandler joinHandler;
	protected ConcurrentHashMap<Long, NetworkWorker> clients = new ConcurrentHashMap<>();
	protected ArrayList<Long> bans = new ArrayList<>();
	protected int maxClients = 1;
	protected long awaitReconnect = 10000; // 10 secs
	protected int connections;

	public Server() {
		joinHandler = new DefaultJoinHandler();
	}

	@Override
	protected void initialize0() throws Throwable {
		internal_bind(9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_banall();
		clients = null;
		bans = null;

		internal_unbind();
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		Utils.sleep(25);
		if (getClientsAmount() != 0) {

			for (Entry<Long, NetworkWorker> entry : clients.entrySet()) {
				try {
					NetworkWorker client = entry.getValue();
					if (!client.isConnected()) {
						continue;
					}

					client.writeLevel();
					client.writeObjectsData();
					client.writeActiveCamera();
					client.writeActiveController();

					// Write / read routine, in this order for client synchronization
					client.writeSocket();
					client.readSocket();

				} catch (Throwable e) {
					Logger.error("Exception occurred in client " + entry.getKey(), e);
					Logger.info("Awaiting reconnection for " + awaitReconnect + " ms");
					entry.getValue().resetSocket();
					entry.getValue().setLostConnectionTime(System.currentTimeMillis());
				}
			}

			for (Iterator<Entry<Long, NetworkWorker>> iterator = clients.entrySet().iterator(); iterator.hasNext();) {
				Entry<Long, NetworkWorker> entry = iterator.next();
				NetworkWorker client = entry.getValue();
				boolean remove = !client.isConnected();
				remove &= System.currentTimeMillis() > client.getLostConnectionTime() + awaitReconnect;
				if (remove) {
					Logger.info("Client " + entry.getKey() + " did not reconnect in time");
					internal_kick(entry.getKey());
					iterator.remove();
				}
			}

		} else if (isBound()) {
			internal_accept();
		}
		if (canCloseServer() && isBound()) {
//			internal_unbind();
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getServer();
	}

	public boolean kick(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_kick(id));
	}

	protected boolean internal_kick(long id) {
		NetworkWorker worker = clients.remove(id);
		if (worker == null) {
			return false;
		}
		worker.destroy();
		Logger.info("Kicked client " + id);
		return true;
	}

	public boolean ban(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_ban(id));
	}

	protected boolean internal_ban(long id) {
		NetworkWorker worker = clients.get(id);
		if (worker != null) {
			Logger.info("Client " + id + " is still connected, kicking first");
			internal_kick(id);
		}
		boolean banned = bans.contains(id);
		if (banned) {
			bans.add(id);
			Logger.info("Banned client " + id);
		} else {
			Logger.warning("Client " + id + " already banned");
		}
		return banned;
	}

	public boolean pardon(long id) {
		return (boolean) getDispatcher().quickQueue(() -> internal_pardon(id));
	}

	protected boolean internal_pardon(long id) {
		boolean unbanned = bans.remove(id);
		if (unbanned) {
			Logger.info("Unbanned client " + id);
		} else {
			Logger.warning("Client " + id + " was not banned");
		}
		return unbanned;
	}

	public boolean kickAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_kickall);
	}

	protected boolean internal_kickall() {
		boolean last = false;
		for (Long id : clients.keySet()) {
			last |= internal_kick(id);
		}
		return last;
	}

	public boolean banAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_banall);
	}

	protected boolean internal_banall() {
		boolean last = false;
		for (Long id : clients.keySet()) {
			last |= internal_ban(id);
		}
		return last;
	}

	public boolean pardonAll() {
		return (boolean) getDispatcher().quickQueue(this::internal_pardonall);
	}

	protected boolean internal_pardonall() {
		int size = bans.size();
		bans.clear();
		return size > 0;
	}

	protected boolean internal_accept() throws IOException {
		// Accept connection
		Socket socket = server.accept();
		if (socket == null) {
			return false;
		}

		// Hash remote ip address
		String ip = ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();
		long hash = 1125899906842597L;

		for (int i = 0; i < ip.length(); i++) {
			hash = 31 * hash + ip.charAt(i);
		}

		// Check if it is already instantiated here and add it
		Long clientId = new Long(hash);
		if (!clients.containsKey(clientId)) {

			// New connection, initialize it
			NetworkWorker client = new NetworkWorker(this);
			client.provideSocket(socket);
			clients.put(clientId, client);

			// Perform handshake
			if (!internal_handshake(client)) {
				Logger.warning("Client handshake failed (" + clientId + ")");
				internal_kick(clientId);
			} else {
				// Perform additional initialization on new connection
				joinHandler.handleJoin(getGame().isClient(), client);

				// Success, return true
				connections++;
				Logger.info("ACCEPTED connection from " + ip + " (" + clientId + ")");
				return true;
			}

		} else {

			// Existing connection, attempt reconnection
			NetworkWorker client = clients.get(clientId);
			if (!client.isConnected()) {
				// Provide new socket to worker
				client.provideSocket(socket);

				// Re-perform handshake for security reasons
				if (!internal_handshake(client)) {
					Logger.warning("Client handshake failed (" + clientId + ")");
					internal_kick(clientId);
				} else {
					// Old connection successfully re-established, return true
					Logger.info("RECONNECTED with client " + ip + " (" + clientId + ")");
					return true;
				}
			} else {
				Logger.warning("Attempt to connect more than once from the same network");
			}
		}

		// Failure, close socket and return false
		Logger.warning("REFUSED connection from " + ip);
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		}
		return false;
	}

	public boolean bind(int port) {
		return (boolean) getDispatcher().quickQueue(() -> internal_bind(port));
	}

	protected boolean internal_bind(int port) {
		if (isBound()) {
			Logger.warning("Server is already bound, stopping first...");
			internal_unbind();
		}
		try {
			server = new ServerSocket(port);
		} catch (IOException e) {
			Logger.error("I/O exception occurred while starting server on port " + port + ", aborting", e);
			return false;
		}
		Logger.info("Server now listening on port " + port);
		return true;
	}

	public boolean unbind() {
		return (boolean) getDispatcher().quickQueue(this::internal_unbind);
	}

	protected boolean internal_unbind() {
		if (!isBound()) {
			Logger.warning("Server is not bound, no need to unbind");
			return false;
		}
		try {
			server.close();
		} catch (IOException e) {
			Logger.error("I/O error occurred while closing server, ignoring");
		} finally {
			server = null;
		}
		Logger.info("Server unbound");
		return true;
	}

	protected boolean internal_handshake(NetworkWorker client) throws IOException {
		client.readSocket();
		client.readAuthentication();
		boolean ret = client.writeAuthentication();
		client.writeSocket();
		return ret;
	}

	// Getters and setters

	public boolean isConnected(long id) {
		NetworkWorker client;
		return (client = clients.get(id)) == null ? false : client.isConnected();
	}

	public int getClientsAmount() {
		return clients.size();
	}

	public long getClientId(NetworkWorker client) {
		for (Entry<Long, NetworkWorker> entry : clients.entrySet()) {
			if (entry.getValue() == client) {
				return entry.getKey();
			}
		}
		return 0;
	}

	public NetworkWorker getClientById(long id) {
		return clients.get(id);
	}

	public String getRemoteIp(long id) {
		return clients.get(id).getRemoteIp();
	}

	public int getRemotePort(long id) {
		return clients.get(id).getRemotePort();
	}

	public String getLocalIp() {
		return server == null ? null : server.getInetAddress().getHostAddress();
	}

	public int getLocalPort() {
		return server == null ? 0 : server.getLocalPort();
	}

	public boolean isBound() {
		return server != null && !server.isClosed() && server.isBound();
	}

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

	protected boolean canCloseServer() {
		return connections >= maxClients;
	}

}