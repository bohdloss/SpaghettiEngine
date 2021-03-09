package com.spaghetti.networking;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.spaghetti.core.*;
import com.spaghetti.utils.*;

public class Server extends CoreComponent {

	protected ServerSocket server;
	protected HashMap<Long, NetworkWorker> clients = new HashMap<>();
	protected ArrayList<Long> bans = new ArrayList<>();

	@Override
	protected void initialize0() throws Throwable {
		internal_bind(9018);
	}

	@Override
	protected void terminate0() throws Throwable {
		internal_banall();

		clients = null;
		bans = null;
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		if (getClientsAmount() != 0) {

			for (Entry<Long, NetworkWorker> entry : clients.entrySet()) {
				try {
					NetworkWorker client = entry.getValue();

					client.writeLevel();

					client.writeSocket();
					client.readSocket();
				} catch (Throwable e) {
					Logger.error("Error occurred in client " + entry.getKey(), e);
					internal_kick(entry.getKey());
				}
			}

		} else {
			internal_accept();
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
		try {
			worker.destroy();
		} catch (IOException e) {
			Logger.error("I/O error occurred while kicking client " + id + ", ignoring", e);
		}
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
			Logger.info("Client " + id + " already banned");
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
			Logger.info("Client " + id + " was not banned");
		}
		return unbanned;
	}

	public boolean kickAll() {
		return (boolean) getDispatcher().quickQueue(() -> internal_kickall());
	}

	protected boolean internal_kickall() {
		boolean last = false;
		for (Long id : clients.keySet()) {
			last |= internal_kick(id);
		}
		return last;
	}

	public boolean banAll() {
		return (boolean) getDispatcher().quickQueue(() -> internal_banall());
	}

	protected boolean internal_banall() {
		boolean last = false;
		for (Long id : clients.keySet()) {
			last |= internal_ban(id);
		}
		return last;
	}

	public boolean pardonAll() {
		return (boolean) getDispatcher().quickQueue(() -> internal_pardonall());
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

		// Check if it is already connected here and add it
		Long clientId = new Long(hash);
		if (!clients.containsKey(clientId)) {
			NetworkWorker client = new NetworkWorker(this);
			client.provideSocket(socket);
			clients.put(clientId, client);

			Logger.info("ACCEPTED connection from " + ip);
			return true;
		}

		Logger.warning("REFUSED connection from " + ip);
		socket.close();
		return false;
	}

	public boolean bind(int port) {
		return (boolean) getDispatcher().quickQueue(() -> internal_bind(port));
	}
	
	protected boolean internal_bind(int port) {
		if(isBound()) {
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
		return (boolean) getDispatcher().quickQueue(() -> internal_unbind());
	}
	
	protected boolean internal_unbind() {
		if(!isBound()) {
			Logger.warning("Server is not bound, no need to unbind");
			return false;
		}
		try {
			server.close();
		} catch (IOException e) {
			Logger.error("I/O error occurred while closing server, ignoring", e);
		} finally {
			server = null;
		}
		Logger.info("Server unbound");
		return true;
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
	
}