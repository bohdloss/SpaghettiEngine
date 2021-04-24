package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkBuffer;

public interface Replicable {

	public abstract void writeDataServer(NetworkBuffer dataBuffer);

	public abstract void readDataServer(NetworkBuffer dataBuffer);

	public abstract void writeDataClient(NetworkBuffer dataBuffer);
	
	public abstract void readDataClient(NetworkBuffer dataBuffer);
	
}
