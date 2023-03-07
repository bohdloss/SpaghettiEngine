package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;
import com.spaghetti.networking.ConnectionManager;

public class OnClientBanned extends GameEvent {

	protected final ConnectionManager client;
	protected final long clientId;
	protected final String reason;

	public OnClientBanned(ConnectionManager client, long clientId, String reason) {
		this.client = client;
		this.clientId = clientId;
		this.reason = reason;
	}

	public ConnectionManager getClient() {
		return client;
	}

	public long getClientId() {
		return clientId;
	}

	public String getReason() {
		return reason;
	}

}
