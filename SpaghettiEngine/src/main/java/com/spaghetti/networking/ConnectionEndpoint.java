package com.spaghetti.networking;

import java.util.HashMap;

import com.spaghetti.core.Game;
import com.spaghetti.interfaces.StringCacher;
import com.spaghetti.utils.Utils;

public abstract class ConnectionEndpoint {

	public static enum Priority {
		NONE, SEND, RECEIVE
	}

	// Member data
	protected long id;

	protected StringCacher strCache;
	protected NetworkBuffer w_buffer;
	protected NetworkBuffer r_buffer;

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

		int bufferSize = Game.getGame().getEngineOption("networkbuffer");
		w_buffer = new NetworkBuffer(strCache, bufferSize);
		r_buffer = new NetworkBuffer(strCache, bufferSize);
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
		w_buffer.clear();
		r_buffer.clear();
	}

	public final NetworkBuffer getWriteBuffer() {
		return w_buffer;
	}

	public final NetworkBuffer getReadBuffer() {
		return r_buffer;
	}

	public final void waitCanReceive() {
		while(!canReceive()) {
			if(!isConnected()) {
				throw new IllegalStateException("Endpoint disconnected while waiting for receive opportunity");
			}
			Utils.sleep(1);
		}
	}

	public final void waitCanSend() {
		while(!canSend()) {
			if(!isConnected()) {
				throw new IllegalStateException("Endpoint disconnected while waiting for send opportunity");
			}
			Utils.sleep(1);
		}
	}

	// Abstract methods
	public abstract void connect(Object socket);

	public abstract void connect(String ip, int port) throws Throwable;

	public abstract void disconnect();

	public final void reconnect() throws Throwable {
		if(isConnected()) {
			String ip = getRemoteIp();
			int port = getRemotePort();
			disconnect();
			connect(ip, port);
		}
	}

	public abstract void send() throws Throwable;

	public abstract void receive() throws Throwable;

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
