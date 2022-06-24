package com.spaghetti.networking;

import java.util.HashMap;
import com.spaghetti.core.*;
import com.spaghetti.interfaces.Serializer;
import com.spaghetti.interfaces.RemoteProcedureCallback;
import com.spaghetti.utils.IdProvider;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class RemoteProcedure {

	protected static final HashMap<Integer, RemoteProcedure> rpcs = new HashMap<>();

	public static void save(RemoteProcedure rpc) {
		rpcs.put(rpc.getId(), rpc);
	}

	public static RemoteProcedure get(int id) {
		return rpcs.remove(id);
	}

	private Serializer<?>[] argSerializers;
	private Object[] arguments;

	private Serializer<?> retSerializer;
	private Object retVal;

	private RemoteProcedureCallback callback;

	private int id;
	private boolean ready;
	private boolean error;
	private boolean reliable;

	public RemoteProcedure() {
		id = IdProvider.newId(Game.getGame());
		ready = true;
		error = false;
		reliable = true;

		Class<?>[] argClasses = getArgumentTypes();
		if (argClasses != null) {
			argSerializers = new Serializer<?>[argClasses.length];
			for (int i = 0; i < argSerializers.length; i++) {
				argSerializers[i] = Serializer.get(argClasses[i]);
			}
		} else {
			argSerializers = new Serializer[0];
		}

		Class<?> retClass = getReturnType();
		if (retClass != null) {
			retSerializer = Serializer.get(retClass);
		} else {
			retSerializer = null;
		}

		arguments = new Object[argSerializers.length];
	}

	public final Object callAndWait(Object... args) {
		call(args);
		return waitCompletion();
	}

	public final synchronized RemoteProcedure call(Object... args) {
		waitCompletion();
		ready = false;

		System.arraycopy(args, 0, arguments, 0, Math.min(args.length, arguments.length));

		Game game = Game.getGame();
		if (game.isClient()) {
			game.getClient().queueRPC(this);
		} else {
			game.getServer().queueRPC(this);
		}
		return this;
	}

	public Object waitCompletion() {
		while (!ready) {
			Utils.sleep(1);
		}
		return error ? null : retVal;
	}

	protected abstract Class<?>[] getArgumentTypes();

	protected abstract Class<?> getReturnType();

	public final void writeArgs(NetworkBuffer buffer) {
		for (int i = 0; i < arguments.length; i++) {
			argSerializers[i].writeClassGeneric(arguments[i], buffer);
		}
	}

	public final void readArgs(NetworkBuffer buffer) {
		for (int i = 0; i < arguments.length; i++) {
			arguments[i] = argSerializers[i].readClassGeneric(arguments[i], buffer);
		}
	}

	public final RemoteProcedure execute(ConnectionManager worker) {
		try {
			retVal = onCall(arguments, worker.player);
			error = false;
		} catch (Throwable t) {
			Logger.error("Error in remote procedure call", t);
			retVal = null;
			error = true;
		}
		return this;
	}

	protected abstract Object onCall(Object[] args, GameObject player) throws Throwable;

	public final void writeReturn(NetworkBuffer buffer) {
		if (!hasReturnValue()) {
			return;
		}
		// Error flag
		buffer.putBoolean(error);
		if (error) {
			return;
		}
		retSerializer.writeClassGeneric(retVal, buffer);
	}

	public final void readReturn(NetworkBuffer buffer) {
		if (!hasReturnValue()) {
			return;
		}
		error = buffer.getBoolean();
		if (error) {
			retVal = null;
			return;
		}
		retVal = retSerializer.readClassGeneric(retVal, buffer);
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

	public final void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	
	public final boolean isReliable() {
		return reliable;
	}
	
	public final Object getReturnValue() {
		return retVal;
	}

	public final boolean hasReturnValue() {
		return retSerializer != null;
	}

	public final RemoteProcedure setCallback(RemoteProcedureCallback callback) {
		this.callback = callback;
		return this;
	}

	public final RemoteProcedure executeReturnCallback() {
		if (callback == null) {
			return this;
		}
		try {
			callback.receiveReturnValue(this, retVal, error, false);
		} catch (Throwable t) {
			Logger.error("Error occurred in RemoteProcedure return callback", t);
		}
		return this;
	}

	public final RemoteProcedure executeAckCallback() {
		if (callback == null) {
			return this;
		}
		try {
			callback.receiveReturnValue(this, null, error, true);
		} catch (Throwable t) {
			Logger.error("Error occurred in RemoteProcedure acknowledgement callback", t);
		}
		return this;
	}

}
