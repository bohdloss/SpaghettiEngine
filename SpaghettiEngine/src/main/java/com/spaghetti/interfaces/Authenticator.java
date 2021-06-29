package com.spaghetti.interfaces;

import com.spaghetti.networking.*;

public interface Authenticator {

	public abstract void w_client_auth(NetworkConnection worker, NetworkBuffer buffer);

	public abstract boolean r_client_auth(NetworkConnection worker, NetworkBuffer buffer);

	public abstract boolean w_server_auth(NetworkConnection worker, NetworkBuffer buffer);

	public abstract void r_server_auth(NetworkConnection worker, NetworkBuffer buffer);

}
