package com.spaghetti.physics;

import org.joml.Vector2d;

import com.spaghetti.core.GameObject;
import com.spaghetti.interfaces.Replicate;

public final class Physics extends GameObject {

	@Replicate
	protected double G = 6.67408 * Math.pow(10, -11);
	@Replicate
	protected double AIR_FRICTION = 18.6;
	@Replicate
	protected Vector2d gravity = new Vector2d(0, -9.81);

	public Physics() {
		body_list = new RigidBody[256];
	}

	protected int body_count;
	protected RigidBody[] body_list;

	// Manage bodies present in the simulated world

	protected void addBody(RigidBody body) {
		if (containsBody(body)) {
			return;
		}
		if (body_count == body_list.length) {
			RigidBody[] old = body_list;
			body_list = new RigidBody[old.length * 2]; // Double size when space is over
			System.arraycopy(old, 0, body_list, 0, old.length);
		}
		body_list[body_count++] = body;
	}

	protected void removeBody(RigidBody body) {
		boolean found = false;
		for (int i = 0; i < body_count; i++) {
			if (body_list[i] == body) {
				found = true;
			}
			if (found && i + 1 < body_count) {
				body_list[i] = body_list[i + 1];
			}
		}
		if (found) {
			body_count--;
		}
	}

	protected boolean containsBody(RigidBody body) {
		for (int i = 0; i < body_count; i++) {
			if (body_list[i] == body) {
				return true;
			}
		}
		return false;
	}

	// Solve all physics calculations

	public void solve(double delta) {
		double multiplier = getGame().getTickMultiplier(delta);
		for (int i = 0; i < body_count; i++) {
			body_list[i].gatherCache();
		}
		for (int i = 0; i < body_count; i++) {
			body_list[i].solve(multiplier);
			body_list[i].applyPosition();
		}
	}

	// Networking

	// TODO

	// Getters

	public int getBodyCount() {
		return body_count;
	}

	public void getGravity(Vector2d pointer) {
		pointer.set(gravity);
	}

	public double getGravitationalConstant() {
		return G;
	}

	public double getAirFriction() {
		return AIR_FRICTION;
	}

	// Setters

	public void setGravity(double x, double y) {
		gravity.set(x, y);
	}

	public void setGravity(Vector2d vec) {
		setGravity(vec.x, vec.y);
	}

	public void setGravitationalConstant(double value) {
		G = value;
	}

	public void setAirFriction(double value) {
		AIR_FRICTION = value;
	}

	// Interfaces

	@Override
	public void commonUpdate(double delta) {
		solve(delta);
	}

}
