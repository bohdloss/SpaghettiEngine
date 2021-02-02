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
	public void getReplicateData(NetworkBuffer buffer) {
	}

	@Override
	public void setReplicateData(NetworkBuffer buffer) {
	}

	@Override
	public final void update(double delta) {
		// TODO: determine whether this is a client or server instance
		clientUpdate(delta);
	}

	public void clientUpdate(double delta) {
	}
	
	public void serverUpdate(double delta) {
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
