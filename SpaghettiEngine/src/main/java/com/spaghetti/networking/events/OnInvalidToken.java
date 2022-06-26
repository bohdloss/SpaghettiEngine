package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;

public class OnInvalidToken extends GameEvent {

	protected long token;
	protected String message;

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
