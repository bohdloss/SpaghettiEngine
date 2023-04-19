package com.spaghetti.networking.tcp;

import com.spaghetti.core.Game;
import com.spaghetti.networking.ClientComponent;
import com.spaghetti.networking.ConnectionEndpoint;

public class TCPClient extends ClientComponent {

	@Override
	public void initialize(Game game) throws Throwable {
		super.initialize(game);
	}

	@Override
	public ConnectionEndpoint internal_connectsocket(String ip, int port) throws Throwable {
		TCPConnection endpoint = new TCPConnection();
		endpoint.connect(ip, port);
		return endpoint;
	}

}
