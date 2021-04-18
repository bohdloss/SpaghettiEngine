package com.spaghetti.networking;

import com.spaghetti.interfaces.Authenticator;

public class DefaultAuthenticator implements Authenticator {

	@Override
	public void w_client_auth(NetworkWorker worker, NetworkBuffer buffer) {

	}

	@Override
	public boolean r_client_auth(NetworkWorker worker, NetworkBuffer buffer) {
		Client client = (Client) worker.getParent();
		long id = buffer.getLong();
		client.clientId = id;
		return true;
	}

	@Override
	public boolean w_server_auth(NetworkWorker worker, NetworkBuffer buffer) {
		Server server = (Server) worker.getParent();
		long id = server.getClientId(worker);
		buffer.putLong(id);
		return true;
	}

	@Override
	public void r_server_auth(NetworkWorker worker, NetworkBuffer buffer) {

	}

}
