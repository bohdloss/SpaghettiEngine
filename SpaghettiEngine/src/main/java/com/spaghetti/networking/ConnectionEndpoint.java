package com.spaghetti.networking;

import java.util.HashMap;

import com.spaghetti.core.Game;
import com.spaghetti.exceptions.EndpointException;
import com.spaghetti.utils.StringCacher;
import com.spaghetti.utils.ThreadUtil;

public abstract class ConnectionEndpoint {

	public static enum Priority {
		NONE, SEND, RECEIVE
	}

	// Member data
	protected long id;

	protected StringCacher strCache;
	protected NetworkBuffer writeBuffer;
	protected NetworkBuffer readBuffer;

	protected boolean reliable;
	protected Priority priority = Priority.NONE;

	public ConnectionEndpoint() {
		strCache = new StringCacher() {
			public HashMap<Short, String> strings = new HashMap<>();

			@Override
			public void cacheString(short hash, String string) {
				strings.put(hash, string);
			}

			@Override
			public String getCachedString(short hash) {
				return strings.get(hash);
			}

			@Override
			public boolean containsString(short hash) {
				return strings.containsKey(hash);
			}
		};

		Integer bufferSize = Game.getInstance().getEngineSetting("network.bufferSize");
		if(bufferSize == null || bufferSize < 1) {
			throw new EndpointException("The engine option for buffer size is missing or invalid");
		}
		writeBuffer = new NetworkBuffer(strCache, bufferSize);
		readBuffer = new NetworkBuffer(strCache, bufferSize);
	}

	public final void setPriority(Priority priority) {
		this.priority = priority;
	}

	public final Priority getPriority() {
		return priority;
	}

	public final boolean isReliable() {
		return reliable;
	}

	public final void setReliable(boolean reliable) {
		this.reliable = reliable;
	}

	public final void clear() {
		writeBuffer.clear();
		readBuffer.clear();
	}

	public final NetworkBuffer getWriteBuffer() {
		return writeBuffer;
	}

	public final NetworkBuffer getReadBuffer() {
		return readBuffer;
	}

	public final void waitCanReceive() {
		while(!canReceive()) {
			if(!isConnected()) {
				throw new EndpointException("Endpoint disconnected while waiting for receive opportunity");
			}
			ThreadUtil.sleep(1);
		}
	}

	public final void waitCanSend() {
		while(!canSend()) {
			if(!isConnected()) {
				throw new EndpointException("Endpoint disconnected while waiting for send opportunity");
			}
			ThreadUtil.sleep(1);
		}
	}

	// Abstract methods
	public abstract void connect(Object socket);

	public abstract void connect(String ip, int port);

	public abstract void disconnect();

	public final void reconnect() {
		if(isConnected()) {
			String ip = getRemoteIp();
			int port = getRemotePort();
			disconnect();
			connect(ip, port);
		}
	}

	public abstract void send();

	public abstract void receive();

	public abstract void destroy();

	// Abstract getters
	public abstract boolean isConnected();

	public abstract String getRemoteIp();

	public abstract int getRemotePort();

	public abstract String getLocalIp();

	public abstract int getLocalPort();

	public abstract boolean canSend();

	public abstract boolean canReceive();

}
