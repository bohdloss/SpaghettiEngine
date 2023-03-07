package com.spaghetti.networking.tcp;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.spaghetti.networking.ConnectionEndpoint;
import com.spaghetti.networking.ServerCore;

public class TCPServer extends ServerCore {

	// Server object
	ServerSocketChannel server;

	// Binding
	@Override
	protected void internal_startserver(int port) throws Throwable {
		server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port));
	}

	// Unbinding
	@Override
	protected void internal_stopserver() throws Throwable {
		ServerSocketChannel s = server;
		server = null;
		s.close();
	}

	// Accepting connections
	@Override
	protected ConnectionEndpoint internal_acceptsocket() throws Throwable {
		// Accept socket
		SocketChannel socket = server.accept();
		if (socket == null) {
			return null;
		}
		socket.configureBlocking(false);

		// Initialize endpoint
		ConnectionEndpoint endpoint = new TCPConnection();
		endpoint.connect(socket);
		return endpoint;
	}

	// Getters

	@Override
	public String getLocalIp() {
		return server == null ? null : server.socket().getInetAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		return server == null ? 0 : server.socket().getLocalPort();
	}

	@Override
	public boolean isBound() {
		return server != null && !server.socket().isClosed() && server.socket().isBound();
	}

}
