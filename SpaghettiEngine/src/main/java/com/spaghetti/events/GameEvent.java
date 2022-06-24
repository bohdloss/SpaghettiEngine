package com.spaghetti.events;

import com.spaghetti.core.Game;
import com.spaghetti.interfaces.Replicable;
import com.spaghetti.networking.NetworkBuffer;
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
		this.id = IdProvider.newId(Game.getGame());
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

	// Override this to mask out certain clients / servers
	@Override
	public boolean needsReplication(ConnectionManager target) {
		return false;
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
	}

}