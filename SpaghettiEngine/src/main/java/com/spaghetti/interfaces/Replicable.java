package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.ConnectionManager;

public interface Replicable {

	public default void writeDataServer(NetworkBuffer dataBuffer) {
	}

	public default void readDataServer(NetworkBuffer dataBuffer) {
	}

	public default void writeDataClient(NetworkBuffer dataBuffer) {
	}

	public default void readDataClient(NetworkBuffer dataBuffer) {
	}

	public default boolean needsReplication(ConnectionManager connection) {
		return true;
	}

	public default boolean isLocal() {
		return false;
	}
	
}
