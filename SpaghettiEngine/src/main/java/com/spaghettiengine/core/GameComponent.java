package com.spaghettiengine.core;

import com.spaghettiengine.interfaces.*;
import com.spaghettiengine.utils.*;

public abstract class GameComponent implements Tickable, Replicable {

	private GameObject owner;
	private boolean destroyed;
	
	public GameComponent(GameObject owner) {
		if(owner == null || owner.isDestroyed()) {
			throw new IllegalArgumentException();
		}
		
		this.owner = owner;
	}
	
	public GameComponent() {
	}
	
	// Interfaces
	
	protected void onBeginPlay() {
	}
	
	protected void onEndPlay() {
	}
	
	protected void onDestroy() {
	}
	
	public void getReplicateData(SpaghettiBuffer buffer) {
	}
	
	public void setReplicateData(SpaghettiBuffer buffer) {
	}
	
	public void update(double delta) {
	}
	
	// Hierarchy methods
	
	public final void destroy() {
		owner.removeComponent(owner.getComponentIndex(this));
		onDestroy();
		owner = null;
		destroyed = true;
	}
	
	protected final boolean isDestroyed() {
		return destroyed;
	}
	
	// Getters and setters
	
	public GameObject getOwner() {
		return owner;
	}
	
	public Level getLevel() {
		return owner.getLevel();
	}
	
}
