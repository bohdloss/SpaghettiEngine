package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;
import com.spaghetti.networking.ConnectionManager;

public class OnClientDisconnect extends GameEvent {

	protected ConnectionManager client;
	protected long clientId;

	public OnClientDisconnect(ConnectionManager client, long clientId) {
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
