package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;

public class OnConnectionRefused extends GameEvent {

	protected long reason;
	protected String message;

	public OnConnectionRefused(long reason, String message) {
		this.reason = reason;
		this.message = message;
	}

	public long getReason() {
		return reason;
	}

	public String getMessage() {
		return message;
	}

}
