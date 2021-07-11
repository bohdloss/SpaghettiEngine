package com.spaghetti.networking.tcp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.networking.Opcode;
import com.spaghetti.utils.Utils;

public class TCPConnection extends NetworkConnection {

	protected Socket socket;
	protected static final int HEADER_SIZE = Integer.BYTES + Short.BYTES;
	protected ByteBuffer packet_header;

	public TCPConnection(CoreComponent parent) {
		super(parent);
		packet_header = ByteBuffer.allocate(HEADER_SIZE);
		packet_header.order(NetworkBuffer.ORDER);
	}

	@Override
	public void destroy() {
		super.destroy();
		packet_header = null;
	}

	@Override
	public void connect(Object socket) {
		if (socket == null || ((Socket) socket).isClosed()) {
			throw new IllegalArgumentException("Invalid socket provided");
		}
		this.socket = (Socket) socket;
		ping = true;
		str_cache.clear();
		w_buffer.clear();
		r_buffer.clear();
	}

	@Override
	public void connect(String ip, int port) throws UnknownHostException, IOException {
		connect(new Socket(ip, port));
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			Utils.close(socket);
			socket = null;
			str_cache.clear();
		}
		this.ping = false;
	}

	@Override
	public void send() throws Throwable {
		// Ensure end instruction to avoid errors
		w_buffer.putByte(Opcode.END);

		OutputStream os = socket.getOutputStream();
		int length = w_buffer.getPosition();

		// Write header and body packet data
		packet_header.clear();
		packet_header.putInt(length);
		packet_header.putShort(Utils.shortHash(w_buffer.asArray(), 0, length));
		os.write(packet_header.array());
		os.write(w_buffer.asArray(), 0, length);
		os.flush();

		// Reset state
		w_buffer.clear();
		reliable = false;
		forceReplication = false;
	}

	@Override
	public void receive() throws Throwable {
		InputStream is = socket.getInputStream();

		// Set timeout to not read forever
		final int timeout = Integer.MAX_VALUE;
		socket.setSoTimeout(timeout);

		// Read header info and then body of packet
		Utils.effectiveReadTimeout(is, packet_header.array(), 0, HEADER_SIZE, timeout);
		packet_header.clear();
		int length = packet_header.getInt();
		short checksum = packet_header.getShort();
		Utils.effectiveReadTimeout(is, r_buffer.asArray(), 0, length, timeout);

		// Check checksum against read data
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
		return socket != null && !socket.isClosed() && ping;
	}

	@Override
	public String getRemoteIp() {
		InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getRemotePort() {
		InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
		return address.getPort();
	}

	@Override
	public String getLocalIp() {
		InetSocketAddress address = (InetSocketAddress) socket.getLocalSocketAddress();
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		InetSocketAddress address = (InetSocketAddress) socket.getLocalSocketAddress();
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