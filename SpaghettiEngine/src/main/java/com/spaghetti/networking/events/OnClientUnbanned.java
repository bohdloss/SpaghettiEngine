package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;
import com.spaghetti.networking.ConnectionManager;

public class OnClientUnbanned extends GameEvent {

	protected final ConnectionManager client;
	protected final long clientId;

	public OnClientUnbanned(ConnectionManager client, long clientId) {
		this.client = client;
		this.clientId = clientId;
	}

	public ConnectionManager getClient() {
		return client;
	}

	public long getClientId() {
		return clientId;
	}

}
