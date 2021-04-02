package com.spaghetti.core;

import java.util.HashMap;

import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;

public abstract class GameComponent implements Tickable, Replicable {

	private static HashMap<Integer, Long> staticId = new HashMap<>();

	private static final synchronized long newId() {
		int index = Game.getGame().getIndex();
		Long id = staticId.get(index);
		if (id == null) {
			id = 0l;
		}
		staticId.put(index, id + 1l);
		return id;
	}

	private GameObject owner;
	private boolean destroyed;
	private boolean attached;
	private long id;

	public GameComponent() {
		this.id = newId();

	}

	// Interfaces

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	@Override
	public void writeData(boolean isClient, NetworkBuffer buffer) {
	}

	@Override
	public void readData(boolean isClient, NetworkBuffer buffer) {
	}

	// All of the warnings from GameObject's
	// update methods apply here as well
	@Override
	public final void update(double delta) {
		commonUpdate(delta);

		if (getGame().isClient()) {
			clientUpdate(delta);
		} else {
			serverUpdate(delta);
		}
	}

	protected void clientUpdate(double delta) {
	}

	protected void serverUpdate(double delta) {
	}

	protected void commonUpdate(double delta) {
	}

	// Hierarchy methods

	public final void destroy() {
		owner.removeComponent(id);
		onDestroy();
		owner = null;
		destroyed = true;
	}

	// Getters and setters

	protected final boolean isDestroyed() {
		return destroyed;
	}

	public final GameObject getOwner() {
		return owner;
	}

	public final Level getLevel() {
		return owner.getLevel();
	}

	public final Game getGame() {
		return owner.getGame();
	}

	public final long getId() {
		return id;
	}

	public final boolean isAttached() {
		return attached;
	}

}
