package com.spaghetti.events;

import com.spaghetti.core.Game;
import com.spaghetti.networking.Replicable;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.utils.IdProvider;

public abstract class GameEvent implements Replicable {

	public static final int CLIENT = -1;
	public static final int NOT_SET = 0;
	public static final int SERVER = 1;

	private final int id;
	private int from;
	private boolean cancelled;

	public GameEvent() {
		this.id = IdProvider.newId(Game.getInstance());
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

	public final int getId() {
		return id;
	}

	public final void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public final boolean isCancelled() {
		return cancelled;
	}

	/**
	 * By default, events don't replicate
	 * Override to change this behaviour
	 *
	 * @return true
	 */
	@Override
	public boolean isLocal() {
		return true;
	}

}