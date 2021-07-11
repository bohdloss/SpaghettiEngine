package com.spaghetti.networking;

import com.spaghetti.interfaces.Authenticator;

public class DefaultAuthenticator implements Authenticator {

	@Override
	public void w_client_auth(NetworkConnection worker, NetworkBuffer buffer) {

	}

	@Override
	public boolean r_client_auth(NetworkConnection worker, NetworkBuffer buffer) {
		ClientCore client = (ClientCore) worker.getCore();
		long id = buffer.getLong();
		client.flags.clientId = id;
		return true;
	}

	@Override
	public boolean w_server_auth(NetworkConnection worker, NetworkBuffer buffer) {
		ServerCore server = (ServerCore) worker.getCore();
		long id = server.getClientId(worker);
		buffer.putLong(id);
		return true;
	}

	@Override
	public void r_server_auth(NetworkConnection worker, NetworkBuffer buffer) {

	}

}
