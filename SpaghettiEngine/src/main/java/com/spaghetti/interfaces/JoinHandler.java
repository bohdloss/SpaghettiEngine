package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkConnection;

public interface JoinHandler {

	public void handleJoin(boolean isClient, NetworkConnection worker);

}
