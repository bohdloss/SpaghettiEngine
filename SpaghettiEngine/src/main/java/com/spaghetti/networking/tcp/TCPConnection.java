package com.spaghetti.networking.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.spaghetti.core.Game;
import com.spaghetti.networking.ConnectionEndpoint;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.NetworkCore;
import com.spaghetti.networking.Opcode;
import com.spaghetti.utils.Utils;

public class TCPConnection extends ConnectionEndpoint {
	
	protected static final int HEADER_SIZE = Integer.BYTES + Short.BYTES;
	
	protected SocketChannel socket;
	protected ByteBuffer packet_header;
	protected ByteBuffer[] composite = new ByteBuffer[2];
	protected long timeout;

	public TCPConnection() {
		packet_header = ByteBuffer.allocate(HEADER_SIZE);
		packet_header.order(NetworkBuffer.ORDER);
	}

	@Override
	public void destroy() {
		packet_header = null;
	}

	@Override
	public void connect(Object obj) {
		SocketChannel socket = (SocketChannel) obj;
		if (socket == null || !socket.isOpen()) {
			throw new IllegalArgumentException("Invalid socket provided");
		}
		this.socket = socket;
		timeout = Game.getGame().getEngineOption("networktimeout");
	}

	@Override
	public void connect(String ip, int port) throws UnknownHostException, IOException {
		SocketChannel socket = SocketChannel.open();
		socket.connect(new InetSocketAddress(ip, port));
		socket.configureBlocking(false);
		connect(socket);
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			Utils.close(socket);
			socket = null;
		}
	}

	@Override
	public void send() throws Throwable {
		// Timeout
		final long begin = System.currentTimeMillis();

		// Ensure end instruction to avoid errors
		w_buffer.putByte(Opcode.END);
		w_buffer.flip();
		int length = w_buffer.getLimit();

		// Write header
		packet_header.clear();
		packet_header.putInt(length);
		packet_header.putShort(Utils.shortHash(w_buffer.asArray(), 0, length));
		packet_header.flip();

		composite[0] = packet_header;
		composite[1] = w_buffer.getRaw();

		while (w_buffer.getFreeSpace() > 0) {
			socket.write(composite, 0, composite.length);
			if (System.currentTimeMillis() > begin + timeout) {
				throw new IllegalStateException(timeout + " ms timeout reached while writing");
			}
		}

		// Reset state
		w_buffer.clear();
		reliable = false;
	}

	@Override
	public void receive() throws Throwable {
		// Timeout
		final long begin = System.currentTimeMillis();

		// Read header info
		packet_header.clear();
		packet_header.limit(HEADER_SIZE);
		while (packet_header.remaining() > 0) {
			socket.read(packet_header);
			if (System.currentTimeMillis() > begin + timeout) {
				throw new IllegalStateException(timeout + " ms timeout reached while reading");
			}
		}
		packet_header.clear();
		int length = packet_header.getInt();
		short checksum = packet_header.getShort();

		if(length < 0 || length > 256000) {
			throw new IllegalStateException("Packet length invalid (" + length + ")");
		}
		
		// Read packet body
		r_buffer.clear();
		r_buffer.setLimit(length);
		while (r_buffer.getFreeSpace() > 0) {
			socket.read(r_buffer.getRaw());
			if (System.currentTimeMillis() > begin + timeout) {
				throw new IllegalStateException(timeout + " ms timeout reached while reading");
			}
		}
		r_buffer.clear();

		// Validate checksum
		if (checksum != Utils.shortHash(r_buffer.asArray(), 0, length)) {
			throw new IllegalStateException("Packet checksum and content do not match");
		}

		// Reset state
		r_buffer.clear();
		r_buffer.setLimit(length);
	}

	// Getters

	@Override
	public boolean isConnected() {
		return socket != null && socket.isOpen();
	}

	@Override
	public String getRemoteIp() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getRemoteAddress();
		} catch (IOException e) {
			throw new RuntimeException("Error getting remote ip", e);
		}
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getRemotePort() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getRemoteAddress();
		} catch (IOException e) {
			throw new RuntimeException("Error getting remote port", e);
		}
		return address.getPort();
	}

	@Override
	public String getLocalIp() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException("Error getting local ip", e);
		}
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getLocalAddress();
		} catch (IOException e) {
			throw new RuntimeException("Error getting local port", e);
		}
		return address.getPort();
	}

	@Override
	public boolean canSend() {
		return true;
	}

	@Override
	public boolean canReceive() {
		return true;
	}

}