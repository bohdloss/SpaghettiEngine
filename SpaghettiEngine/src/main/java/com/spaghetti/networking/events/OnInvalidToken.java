package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;

public class OnInvalidToken extends GameEvent {

	protected final long token;
	protected final String message;

	public OnInvalidToken(long token, String message) {
		this.token = token;
		this.message = message;
	}

	public long getToken() {
		return token;
	}

	public String getMessage() {
		return message;
	}

}
