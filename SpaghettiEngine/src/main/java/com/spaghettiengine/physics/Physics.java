package com.spaghettiengine.physics;

import org.joml.Vector2d;

import com.spaghettiengine.core.GameObject;
import com.spaghettiengine.core.Level;

public final class Physics extends GameObject{
	
	protected double G = 6.67408 * Math.pow(10, -11);
	
	public Physics(Level level) {
		super(level, (GameObject) null);
	}

	// Box2d-style body listing
	protected int body_count;
	protected RigidBody body_list;
	protected Vector2d gravity = new Vector2d(0, -9.81);
	
	// Manage bodies present in the simulated world
	
	protected void addBody(RigidBody body) {
		if(body_list == null) {
			body_list = body;
		} else {
			body_list.previous = body;
			body.next = body_list;
			body_list = body;
		}
		body_count++;
	}
	
	protected void removeBody(RigidBody body) {
		if(body_list == null) {
			return;
		} else {
			RigidBody b = body_list;
			do {
				if(b == body) {
					if(b.previous != null) {
						b.previous.next = b.next;
					}
					if(b.next != null) {
						b.next.previous = b.previous;
					}
					b.previous = null;
					b.next = null;
				}
			} while((b = b.next) != null);
			
		}
	}
	
	// Solve all physics calculations
	
	public void solve(double delta) {
		RigidBody body = body_list;
		double multiplier = getGame().getTickMultiplier(delta);
		do {
			body.gatherCache();
		} while((body = body.next) != null);
		body = body_list;
		do {
			body.solve(multiplier);
			body.applyPosition();
		} while((body = body.next) != null);
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
	
	// Interfaces
	
	@Override
	public void clientUpdate(double delta) {
		solve(delta);
	}
	
}
