package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkWorker;

public interface JoinHandler {

	public void handleJoin(boolean isClient, NetworkWorker worker);

}
