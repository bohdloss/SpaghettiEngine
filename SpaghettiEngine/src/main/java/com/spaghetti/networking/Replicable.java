package com.spaghetti.networking;

import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;

public interface Replicable {

	public default void writeDataServer(ConnectionManager connection, NetworkBuffer dataBuffer) {
	}

	public default void readDataServer(ConnectionManager connection, NetworkBuffer dataBuffer) {
	}

	public default void writeDataClient(ConnectionManager connection, NetworkBuffer dataBuffer) {
	}

	public default void readDataClient(ConnectionManager connection, NetworkBuffer dataBuffer) {
	}

	public default boolean needsReplication(ConnectionManager connection) {
		return true;
	}

	public default boolean isLocal() {
		return false;
	}

}
