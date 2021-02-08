package com.spaghettiengine.core;

import com.spaghettiengine.interfaces.*;
import com.spaghettiengine.utils.*;

public abstract class GameComponent implements Tickable, Replicable {

	private GameObject owner;
	private boolean destroyed;

	public GameComponent() {
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
		owner.removeComponent(owner.getComponentIndex(this));
		onDestroy();
		owner = null;
		destroyed = true;
	}

	// Getters and setters

	protected final boolean isDestroyed() {
		return destroyed;
	}

	public GameObject getOwner() {
		return owner;
	}

	public Level getLevel() {
		return owner.getLevel();
	}

	public Game getGame() {
		return owner.getGame();
	}

}
