package com.spaghetti.networking.tcp;

import com.spaghetti.networking.ClientCore;
import com.spaghetti.networking.ConnectionEndpoint;

public class TCPClient extends ClientCore {

	@Override
	public void initialize0() throws Throwable {
		super.initialize0();
	}

	@Override
	public ConnectionEndpoint internal_connectsocket(String ip, int port) throws Throwable {
		TCPConnection endpoint = new TCPConnection();
		endpoint.connect(ip, port);
		return endpoint;
	}

}
