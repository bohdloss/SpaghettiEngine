package com.spaghetti.core;

import java.util.HashMap;

import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;

public abstract class GameComponent implements Tickable, Replicable {

	private static HashMap<Integer, Long> staticId = new HashMap<>();

	private GameObject owner;
	private boolean destroyed;
	private long id;

	public GameComponent() {

		// Id calculation based on game instance

		int index = Game.getGame().getIndex();

		Long id = staticId.get(index);

		if (id == null) {
			id = 0l;
		}
		this.id = id;
		staticId.put(index, id + 1l);

	}

	// Interfaces

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	@Override
	public void writeData(NetworkBuffer buffer) {
	}

	@Override
	public void readData(NetworkBuffer buffer) {
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

}
