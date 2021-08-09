package com.spaghetti.physics;

import com.spaghetti.core.GameObject;
import com.spaghetti.interfaces.ToClient;

@ToClient
public abstract class Physics extends GameObject {

	public Physics() {
	}
	
	// Physics calculations

	public abstract void solve(float delta);
	public abstract void raycast(RaycastRequest<?, ?> request);

	// Networking is managed by the RigidBody's and the component system

	// World management

	public abstract int getBodyCount();
	public abstract RigidBody<?, ?> getBodyAt(int index);

	// Getters and setters to implement

	public abstract void getGravity(Object pointer);
	public abstract void setGravity(Object vec);

	// Interfaces

	@Override
	public void commonUpdate(float delta) {
		solve(delta);
	}

}
