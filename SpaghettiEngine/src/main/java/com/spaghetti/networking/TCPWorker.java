package com.spaghetti.networking;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.utils.Utils;

public class TCPWorker extends NetworkWorker {

	protected Socket socket;

	public TCPWorker(CoreComponent parent) {
		super(parent);
	}

	@Override
	public void provideSocket(Object sock) {
		Socket socket = (Socket) sock;
		if (socket.isClosed() || socket == null) {
			throw new IllegalArgumentException("Invalid socket provided");
		}
		this.socket = socket;
		ping = true;
		str_cache.clear();
	}

	@Override
	public void resetSocket() {
		if (socket != null) {
			Socket s = this.socket;
			this.socket = null;
			Utils.socketClose(s);
			str_cache.clear();
		}
	}

	@Override
	public void writeSocket() throws Throwable {
		w_buffer.putByte(Opcode.END);

		OutputStream os = socket.getOutputStream();
		int length = w_buffer.getPosition();

		length_b[0] = (byte) ((length >> 24) & 0xff);
		length_b[1] = (byte) ((length >> 16) & 0xff);
		length_b[2] = (byte) ((length >> 8) & 0xff);
		length_b[3] = (byte) (length & 0xff);

		os.write(length_b);
		os.write(w_buffer.asArray(), 0, length);
		os.flush();

		w_buffer.clear();
		reliable = false;
		forceReplication = false;
	}

	@Override
	public void readSocket() throws Throwable {
		InputStream is = socket.getInputStream();
		Utils.effectiveRead(is, length_b, 0, 4);
		int length = length_b[3] & 0xFF | (length_b[2] & 0xFF) << 8 | (length_b[1] & 0xFF) << 16
				| (length_b[0] & 0xFF) << 24;

		Utils.effectiveRead(is, r_buffer.asArray(), 0, length);
		r_buffer.setPosition(0);
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

}
