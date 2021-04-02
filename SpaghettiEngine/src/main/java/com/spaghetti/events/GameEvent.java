package com.spaghetti.events;

import java.util.HashMap;

import com.spaghetti.core.Game;
import com.spaghetti.networking.NetworkWorker;

public abstract class GameEvent {

	private static final HashMap<Integer, Long> staticId = new HashMap<>();

	private static final synchronized long newId() {
		int index = Game.getGame().getIndex();
		Long id = staticId.get(index);
		if (id == null) {
			id = 0l;
		}
		staticId.put(index, id + 1l);
		return id;
	}

	public static final int CLIENT = -1;
	public static final int NOT_SET = 0;
	public static final int SERVER = 1;

	private final long id;
	private int from;
	private boolean cancelled;

	public GameEvent() {
		this.id = newId();
	}

	// Getters and setters

	public final void setFrom(int from) {
		if (this.from == NOT_SET) {
			this.from = from;
		}
	}

	public final int getFrom() {
		return from;
	}

	public final long getId() {
		return id;
	}

	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public final boolean isCancelled() {
		return cancelled;
	}

	// Override this to mask out certain clients / servers
	public boolean skip(NetworkWorker target, boolean isClient) {
		return false;
	}

}