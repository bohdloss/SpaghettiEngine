package com.spaghetti.interfaces;

import com.spaghetti.networking.NetworkWorker;

public interface JoinHandler {

	public void handle(boolean isClient, NetworkWorker worker);

}
