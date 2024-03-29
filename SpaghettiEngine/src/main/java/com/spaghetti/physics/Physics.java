package com.spaghetti.physics;

import com.spaghetti.world.GameObject;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;

/**
 * Physics is the base class for any n-dimensional physics library
 *
 * @author bohdloss
 */
public abstract class Physics<VecType, SecVecType, BodyClass extends RigidBody<VecType, SecVecType>> extends GameObject {

	protected float framerate = 60;

	public Physics() {
	}

	// Physics calculations

	/**
	 * Advance the physics simulation by a given amount of time
	 *
	 * @param delta The time in milliseconds
	 */
	public abstract void solve(float delta);

	/**
	 * Cast a ray from {@code request.beginning} to {@code request.end} and save the
	 * results in {@code request.hits}
	 *
	 * @param request The {@link RaycastRequest} object
	 */
	public abstract void raycast(RaycastRequest<VecType, SecVecType, BodyClass> request);

	// Networking is managed by the RigidBody's and the component system

	// World management

	/**
	 * Retrieves the amount of physic bodies currently in the world
	 *
	 * @return The amount of bodies
	 */
	public abstract int getBodyCount();

	/**
	 * Retrieves the physic body at the given index in the body list
	 *
	 * @param index The index of the body to retrieve
	 * @return The body at that position
	 */
	public abstract BodyClass getBodyAt(int index);

	// Getters and setters to implement

	/**
	 * Retrieves the world's current gravitational acceleration vector
	 *
	 * @param pointer The vector the gravity value will be saved in
	 */
	public abstract void getGravity(VecType pointer);

	/**
	 * Changes the world's gravitational acceleration vector
	 *
	 * @param vec The new gravity vector
	 */
	public abstract void setGravity(VecType vec);

	public float getFramerate() {
		return framerate;
	}

	public void setFramerate(float framerate) {
		this.framerate = framerate;
	}

	// Interfaces

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		buffer.putFloat(framerate);
	}

	@Override
	public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		framerate = buffer.getFloat();
	}

	@Override
	public void commonUpdate(float delta) {
		solve(delta);
	}

}
