package com.spaghetti.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
		channel.register(selector, channel.validOps());
	}

	public AsyncSocket accept() throws IOException {
		selector.select();
		Set<SelectionKey> keys = selector.selectedKeys();
		Iterator<SelectionKey> iterator = keys.iterator();

		while (iterator.hasNext()) {
			SelectionKey key = iterator.next();
			if (key.isAcceptable()) {
				SocketChannel schannel = channel.accept();
				if (schannel == null) {
					continue;
				}
				return new AsyncSocket(selector, schannel);
			}
		}
		return null;
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
