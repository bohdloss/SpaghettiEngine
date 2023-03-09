package com.spaghetti.networking.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.spaghetti.core.Game;
import com.spaghetti.exceptions.EndpointException;
import com.spaghetti.networking.ConnectionEndpoint;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.Opcode;
import com.spaghetti.utils.HashUtil;
import com.spaghetti.utils.StreamUtil;

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
			throw new EndpointException("Invalid socket provided");
		}
		this.socket = socket;
		Long timeout_option = Game.getInstance().getEngineSetting("networktimeout");
		if(timeout_option == null || timeout_option == 0) {
			throw new EndpointException("The engine option for timeout time is missing or invalid");
		}
	}

	@Override
	public void connect(String ip, int port) {
		try {
			SocketChannel socket = SocketChannel.open();
			socket.connect(new InetSocketAddress(ip, port));
			socket.configureBlocking(false);
		} catch(UnknownHostException e) {
			throw new EndpointException("Unknown host \"" + ip + ":" + port + "\"", e);
		} catch(IOException e ) {
			throw new EndpointException("Input / Output error occurred Swhile connecting", e);
		}
		connect(socket);
	}

	@Override
	public void disconnect() {
		if (socket != null) {
			StreamUtil.close(socket);
			socket = null;
		}
	}

	@Override
	public void send() {
		// Timeout
		final long begin = System.currentTimeMillis();

		// Ensure end instruction to avoid errors
		writeBuffer.putByte(Opcode.END);
		writeBuffer.flip();
		int length = writeBuffer.getLimit();

		// Write header
		packet_header.clear();
		packet_header.putInt(length);
		packet_header.putShort(HashUtil.shortHash(writeBuffer.asArray(), 0, length));
		packet_header.flip();

		composite[0] = packet_header;
		composite[1] = writeBuffer.getRaw();

		try {
			while (writeBuffer.getFreeSpace() > 0) {
				socket.write(composite, 0, composite.length);
				if (System.currentTimeMillis() > begin + timeout) {
					throw new EndpointException(timeout + " ms timeout reached while writing");
				}
			}
		} catch(IOException e) {
			throw new EndpointException("Input / Output error occurred while sending a packet", e);
		}

		// Reset state
		writeBuffer.clear();
		reliable = false;
	}

	@Override
	public void receive() {
		// Timeout
		final long begin = System.currentTimeMillis();

		// Read header info
		packet_header.clear();
		packet_header.limit(HEADER_SIZE);

		// Guarded read operation
		try {
			while (packet_header.remaining() > 0) {
				socket.read(packet_header);
				if (System.currentTimeMillis() > begin + timeout) {
					throw new EndpointException(timeout + " ms timeout reached while reading");
				}
			}
		} catch(IOException e) {
			throw new EndpointException("Input / Output error occurred while receiving a packet header", e);
		}

		packet_header.clear();
		int length = packet_header.getInt();
		short checksum = packet_header.getShort();

		if(length < 0 || length > 256000) {
			throw new EndpointException("Packet length invalid (" + length + ")");
		}

		// Read packet body
		readBuffer.clear();
		readBuffer.setLimit(length);

		// Guarded read operation
		try {
			while (readBuffer.getFreeSpace() > 0) {
				socket.read(readBuffer.getRaw());
				if (System.currentTimeMillis() > begin + timeout) {
					throw new EndpointException(timeout + " ms timeout reached while reading");
				}
			}
		} catch(IOException e) {
			throw new EndpointException("Input / Output error occurred while receving a packet body", e);
		}

		readBuffer.clear();

		// Validate checksum
		if (checksum != HashUtil.shortHash(readBuffer.asArray(), 0, length)) {
			throw new EndpointException("Packet checksum and content do not match");
		}

		// Reset state
		readBuffer.clear();
		readBuffer.setLimit(length);
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
			throw new EndpointException("Error getting remote ip", e);
		}
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getRemotePort() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getRemoteAddress();
		} catch (IOException e) {
			throw new EndpointException("Error getting remote port", e);
		}
		return address.getPort();
	}

	@Override
	public String getLocalIp() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getLocalAddress();
		} catch (IOException e) {
			throw new EndpointException("Error getting local ip", e);
		}
		return address.getAddress().getHostAddress();
	}

	@Override
	public int getLocalPort() {
		InetSocketAddress address;
		try {
			address = (InetSocketAddress) socket.getLocalAddress();
		} catch (IOException e) {
			throw new EndpointException("Error getting local port", e);
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