package com.spaghetti.networking.events;

import com.spaghetti.events.GameEvent;

public class OnPing extends GameEvent {

	protected long ping;

	public OnPing(long ping) {
		this.ping = ping;
	}

	public long getPing() {
		return ping;
	}

}
