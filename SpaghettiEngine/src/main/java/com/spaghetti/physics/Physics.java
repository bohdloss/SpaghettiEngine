package com.spaghetti.physics;

import org.joml.Vector2d;

import com.spaghetti.core.GameObject;
import com.spaghetti.core.Level;
import com.spaghetti.interfaces.Replicate;

public final class Physics extends GameObject {

	@Replicate
	protected double G = 6.67408 * Math.pow(10, -11);
	@Replicate
	protected double AIR_FRICTION = 18.6;
	@Replicate
	protected Vector2d gravity = new Vector2d(0, -9.81);

	// Ignores parent!!!
	public Physics(Level level) {
		super(level);
	}

	// Box2d-style body listing
	protected int body_count;
	protected RigidBody body_list;

	// Manage bodies present in the simulated world

	protected void addBody(RigidBody body) {
		if (body_list == null) {
			body_list = body;
		} else {
			body_list.previous = body;
			body.next = body_list;
			body_list = body;
		}
		body_count++;
	}

	protected void removeBody(RigidBody body) {
		if (body_list == null) {
			return;
		} else {
			RigidBody b = body_list;
			do {
				if (b == body) {
					if (b.previous != null) {
						b.previous.next = b.next;
					}
					if (b.next != null) {
						b.next.previous = b.previous;
					}
					b.previous = null;
					b.next = null;
				}
			} while ((b = b.next) != null);

		}
	}

	// Solve all physics calculations

	public void solve(double delta) {
		RigidBody body = body_list;
		double multiplier = getGame().getTickMultiplier(delta);
		do {
			body.gatherCache();
		} while ((body = body.next) != null);
		body = body_list;
		do {
			body.solve(multiplier);
			body.applyPosition();
		} while ((body = body.next) != null);
	}

	// Getters

	public RigidBody getBodies() {
		return body_list;
	}

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
