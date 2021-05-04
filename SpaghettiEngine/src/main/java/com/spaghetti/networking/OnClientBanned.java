package com.spaghetti.networking;

import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.NoReplicate;

@NoReplicate
public class OnClientBanned extends GameEvent {

	protected NetworkWorker client;
	protected long clientId;

	public OnClientBanned(NetworkWorker client, long clientId) {
		this.client = client;
		this.clientId = clientId;
	}

	public NetworkWorker getClient() {
		return client;
	}

	public long getClientId() {
		return clientId;
	}

	@Override
	public boolean skip(NetworkWorker worker, boolean isClient) {
		return true;
	}

}
