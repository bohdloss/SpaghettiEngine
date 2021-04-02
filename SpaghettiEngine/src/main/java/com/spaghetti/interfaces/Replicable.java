package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkBuffer;

public interface Replicable {

	public abstract void writeData(boolean isClient, NetworkBuffer dataBuffer);

	public abstract void readData(boolean isClient, NetworkBuffer dataBuffer);

}
