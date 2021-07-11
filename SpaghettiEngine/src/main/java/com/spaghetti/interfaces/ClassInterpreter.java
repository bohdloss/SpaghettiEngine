package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkBuffer;

public interface ClassInterpreter<T> {

	public abstract void writeClass(T object, NetworkBuffer buffer);

	@SuppressWarnings("unchecked")
	public default void writeClassGeneric(Object object, NetworkBuffer buffer) {
		writeClass((T) object, buffer);
	}

	public abstract T readClass(T original, NetworkBuffer buffer);

	@SuppressWarnings("unchecked")
	public default Object readClassGeneric(Object original, NetworkBuffer buffer) {
		return readClass((T) original, buffer);
	}

}
