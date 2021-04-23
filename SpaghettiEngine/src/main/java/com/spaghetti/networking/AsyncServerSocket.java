package com.spaghetti.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class AsyncServerSocket {

	protected Selector selector;
	protected ServerSocketChannel channel;
	protected ServerSocket server;

	public AsyncServerSocket(int port) throws IOException {
		this.channel = ServerSocketChannel.open();
		this.server = channel.socket();
		this.selector = Selector.open();
		server.bind(new InetSocketAddress("localhost", port));
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_ACCEPT);
	}

	public Socket accept() throws IOException {
		if (selector.select(1) == 0) {
			return null;
		}
		Set<SelectionKey> keys = selector.selectedKeys();
		Iterator<SelectionKey> iterator = keys.iterator();

		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			if (key.isAcceptable()) {
				SocketChannel schannel = channel.accept();
				if (schannel == null) {
					continue;
				}
				iterator.remove();
				return schannel.socket();
			}
		}
		return null;
	}

	public void close() throws IOException {
		channel.close();
		server.close();
	}

	// Getters and setters

	public Selector getSelector() {
		return selector;
	}

	public ServerSocketChannel getServerSocketChannel() {
		return channel;
	}

	public ServerSocket getServerSocket() {
		return server;
	}

}
