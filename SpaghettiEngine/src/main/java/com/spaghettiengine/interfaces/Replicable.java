package com.spaghettiengine.interfaces;

import com.spaghettiengine.utils.NetworkBuffer;

public interface Replicable {

	public abstract void writeData(NetworkBuffer dataBuffer);

	public abstract void readData(NetworkBuffer dataBuffer);

}
