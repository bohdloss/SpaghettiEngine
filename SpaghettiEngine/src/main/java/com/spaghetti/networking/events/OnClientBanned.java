package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.NoReplicate;
import com.spaghetti.networking.NetworkConnection;

@NoReplicate
public class OnClientBanned extends GameEvent {

	protected NetworkConnection client;
	protected long clientId;

	public OnClientBanned(NetworkConnection client, long clientId) {
		this.client = client;
		this.clientId = clientId;
	}

	public NetworkConnection getClient() {
		return client;
	}

	public long getClientId() {
		return clientId;
	}

	@Override
	public boolean needsReplication(NetworkConnection worker) {
		return false;
	}

}
