package com.spaghetti.networking.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.utils.Utils;

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
	protected Object internal_acceptsocket() throws Throwable {
		SocketChannel socket = server.accept();
		if (socket == null) {
			return null;
		}
		socket.configureBlocking(false);
		return socket;
	}

	@Override
	protected long internal_hashsocket(Object object) {
		try {
			SocketChannel socket = (SocketChannel) object;
			InetSocketAddress addr;
			addr = (InetSocketAddress) socket.getRemoteAddress();
			return Utils.longHash(addr.getHostName());// + ":" + addr.getPort());
		} catch (IOException e) {
			throw new RuntimeException("IOException occurred while getting remote socket address", e);
		}
	}

	@Override
	protected void internal_closesocket(Object object) throws Throwable {
		Utils.close((Socket) object);
	}

	@Override
	protected NetworkConnection internal_initworker() {
		return new TCPConnection(this);
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
