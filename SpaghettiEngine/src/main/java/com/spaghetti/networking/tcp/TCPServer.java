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
	protected void internal_startserver(int port) throws Throwable {
		server = new AsyncServerSocket(port);
	}
	
	// Unbinding
	protected void internal_stopserver() throws Throwable {
		AsyncServerSocket s = server;
		server = null;
		s.close();
	}
	
	// Accepting connections
	protected Object internal_acceptsocket() throws Throwable {
		return server.accept();
	}
	
	protected long internal_hashsocket(Object object) {
		Socket socket = (Socket) object;
		InetSocketAddress addr = (InetSocketAddress) socket.getRemoteSocketAddress();
		return Utils.longHash(addr.getHostName() + ":" + addr.getPort());
	}
	
	protected void internal_closesocket(Object object) throws Throwable {
		Utils.close((Socket) object);
	}
	
	protected NetworkConnection internal_initworker() {
		return new TCPConnection(this);
	}
	
	// Getters
	
	public String getLocalIp() {
		return server == null ? null : server.getServerSocket().getInetAddress().getHostAddress();
	}

	public int getLocalPort() {
		return server == null ? 0 : server.getServerSocket().getLocalPort();
	}

	public boolean isBound() {
		return server != null && !server.getServerSocket().isClosed() && server.getServerSocket().isBound();
	}
	
}
