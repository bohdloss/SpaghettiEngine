package com.spaghetti.networking;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

// Unused class, might be integrated at a later stage

public class AsyncSocket {

	protected Selector selector;
	protected SocketChannel channel;
	protected Socket socket;

	public AsyncSocket(String ip, int port) throws IOException {
		if (ip == null) {
			throw new IllegalArgumentException();
		}
		this.channel = SocketChannel.open();
		this.socket = channel.socket();
		this.selector = Selector.open();
		socket.bind(new InetSocketAddress(ip, port));
		channel.configureBlocking(false);
		channel.register(selector, channel.validOps());
	}

	public AsyncSocket(Selector server_sel, SocketChannel schannel) throws IOException {
		if (schannel == null || server_sel == null) {
			throw new IllegalArgumentException();
		}
		schannel.configureBlocking(false);
		schannel.register(server_sel, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		this.channel = schannel;
		this.socket = schannel.socket();
		this.selector = server_sel;
	}

	// Getters and setters

	public Selector getSelector() {
		return selector;
	}

	public SocketChannel getServerSocketChannel() {
		return channel;
	}

	public Socket getSocket() {
		return socket;
	}

}
