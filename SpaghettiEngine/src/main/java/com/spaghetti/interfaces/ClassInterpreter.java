package com.spaghetti.interfaces;

import java.lang.reflect.Field;

import com.spaghetti.networking.NetworkBuffer;

public interface ClassInterpreter {

	public abstract void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException; // Write
																														// to
																														// buffer

	public abstract void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException; // Read
																													// from
																													// buffer

}
