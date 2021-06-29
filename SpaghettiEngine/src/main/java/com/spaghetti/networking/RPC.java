package com.spaghetti.networking;

import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.ClassInterpreter;
import com.spaghetti.interfaces.RPCCallback;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class RPC {

	protected static final HashMap<Integer, RPC> rpcs = new HashMap<>();
	protected static final ThreadLocalRandom random = ThreadLocalRandom.current();

	public static void save(RPC rpc) {
		rpcs.put(rpc.getId(), rpc);
	}
	
	public static RPC get(int id) {
		return rpcs.remove(id);
	}
	
	private final ClassInterpreter<?>[] argInterpreter = getArgInterpreters();
	private final Object[] args = new Object[argInterpreter.length];
	private final int argAmount = args.length;

	private final boolean hasResponse = hasResponse();
	private final ClassInterpreter<?> retInterpreter = getReturnInterpreter();
	private Object retVal;
	private RPCCallback callback;

	private volatile int id;
	private volatile boolean ready;
	private volatile boolean error;
	
	public final Object callAndWait(Object... args) {
		call(args);
		while (!ready) {
			Utils.sleep(1);
		}
		return error ? null : retVal;
	}

	public final RPC call(Object... args) {
		if (args.length != argAmount) {
			throw new IllegalArgumentException(args.length + " arguments provided, expected " + argAmount);
		}

		System.arraycopy(args, 0, this.args, 0, this.args.length);
		this.id = random.nextInt();
		this.ready = false;

		Game game = Game.getGame();
		if (game.isClient()) {
			game.getClient().queueRPC(this);
		} else {
			game.getServer().queueRPC(this);
		}
		return this;
	}

	protected abstract ClassInterpreter<?>[] getArgInterpreters();

	protected abstract ClassInterpreter<?> getReturnInterpreter();

	protected abstract boolean hasResponse();

	public final void writeArgs(NetworkBuffer buffer) {
		for (int i = 0; i < argAmount; i++) {
			argInterpreter[i].writeClassGeneric(args[i], buffer);
		}
	}

	public final void readArgs(NetworkBuffer buffer) {
		for (int i = 0; i < argAmount; i++) {
			args[i] = argInterpreter[i].readClassGeneric(args[i], buffer);
		}
	}

	public final RPC execute(NetworkConnection worker) {
		try {
			retVal = execute0(worker.getGame().isClient(), worker, args);
			error = false;
		} catch (Throwable t) {
			Logger.error("Error in remote procedure call", t);
			retVal = null;
			error = true;
		}
		return this;
	}

	protected abstract Object execute0(boolean isClient, NetworkConnection worker, Object[] args) throws Throwable;

	public final void writeReturn(NetworkBuffer buffer) {
		if (!hasResponse) {
			return;
		}
		// Error flag
		buffer.putBoolean(error);
		if (error) {
			return;
		}
		retInterpreter.writeClassGeneric(retVal, buffer);
	}

	public final void readReturn(NetworkBuffer buffer) {
		if (!hasResponse) {
			return;
		}
		error = buffer.getBoolean();
		if (error) {
			retVal = null;
			return;
		}
		retVal = retInterpreter.readClassGeneric(retVal, buffer);
	}

	public boolean skip(NetworkConnection worker, boolean isClient) {
		return false;
	}

	// Getters

	public final int getId() {
		return id;
	}

	public final boolean isReady() {
		return ready;
	}

	public final boolean isError() {
		return error;
	}

	public final Object getReturnValue() {
		return retVal;
	}

	public final boolean hasReturnValue() {
		return hasResponse;
	}

	public final RPC setCallback(RPCCallback callback) {
		this.callback = callback;
		return this;
	}

	public final RPC executeReturnCallback() {
		if (callback == null) {
			return this;
		}
		try {
			callback.receiveReturnValue(this, retVal, error, false);
		} catch (Throwable t) {
			Logger.error("Error occurred in RPC return callback", t);
		}
		return this;
	}

	public final RPC executeAckCallback() {
		if (callback == null) {
			return this;
		}
		try {
			callback.receiveReturnValue(this, null, error, true);
		} catch (Throwable t) {
			Logger.error("Error occurred in RPC ack callback", t);
		}
		return this;
	}

}
