package com.spaghetti.networking.tcp;

import java.net.InetSocketAddress;
import java.net.Socket;

import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.utils.Utils;

public class TCPServer extends ServerCore {

	// Server instance
	AsyncServerSocket server;

	// Binding
	@Override
	protected void internal_startserver(int port) throws Throwable {
		server = new AsyncServerSocket(port);
	}

	// Unbinding
	@Override
	protected void internal_stopserver() throws Throwable {
		AsyncServerSocket s = server;
		server = null;
		s.close();
	}

	// Accepting connections
	@Override
	protected Object internal_acceptsocket() throws Throwable {
		return server.accept();
	}

	@Override
	protected long internal_hashsocket(Object object) {
		Socket socket = (Socket) object;
		InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
		return Utils.longHash(addr.getHostName() + ":" + addr.getPort());
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
		return server == null ? null : server.getServerSocket().getInetAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		return server == null ? 0 : server.getServerSocket().getLocalPort();
	}

	@Override
	public boolean isBound() {
		return server != null && !server.getServerSocket().isClosed() && server.getServerSocket().isBound();
	}

}
