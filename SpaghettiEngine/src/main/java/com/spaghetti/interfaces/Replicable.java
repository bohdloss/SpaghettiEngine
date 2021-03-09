package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkBuffer;

public interface Replicable {

	public abstract void writeData(NetworkBuffer dataBuffer);

	public abstract void readData(NetworkBuffer dataBuffer);

}
