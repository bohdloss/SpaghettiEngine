package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;
import com.spaghetti.networking.ConnectionManager;

public class OnClientBanned extends GameEvent {

	protected ConnectionManager client;
	protected long clientId;
	protected String reason;

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
