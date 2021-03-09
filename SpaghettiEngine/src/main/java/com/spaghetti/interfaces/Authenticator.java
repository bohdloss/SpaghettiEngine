package com.spaghetti.interfaces;

import com.spaghetti.networking.*;

public interface Authenticator {

	public abstract void w_client_auth(NetworkWorker worker, NetworkBuffer buffer);

	public abstract void r_client_auth(NetworkWorker worker, NetworkBuffer buffer);

	public abstract void w_server_auth(NetworkWorker worker, NetworkBuffer buffer);

	public abstract void r_server_auth(NetworkWorker worker, NetworkBuffer buffer);

}
