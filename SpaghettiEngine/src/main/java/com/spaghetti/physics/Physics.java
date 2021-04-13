package com.spaghetti.physics;

import org.joml.Vector2d;

import com.spaghetti.core.GameObject;
import com.spaghetti.core.Level;
import com.spaghetti.interfaces.Replicate;
import com.spaghetti.networking.NetworkBuffer;

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
			while(b != null) {
				if (b == body) {
					if (b.previous != null) {
						b.previous.next = b.next;
					}
					if (b.next != null) {
						b.next.previous = b.previous;
					}
					b.previous = null;
					b.next = null;
					break;
				}
				b = b.next;
			}

		}
	}

	protected boolean containsBody(RigidBody body) {
		if(body_list == null) {
			return false;
		} else {
			RigidBody b = body_list;
			while(b != null) {
				if(b.getId() == body.getId()) {
					return true;
				}
				b = b.next;
			}
		}
		return false;
	}
	
	// Solve all physics calculations

	public void solve(double delta) {
		RigidBody body = body_list;
		double multiplier = getGame().getTickMultiplier(delta);
		while(body != null) {
			body.gatherCache();
			body = body.next;
		}
		body = body_list;
		while(body != null) {
			body.solve(multiplier);
			body.applyPosition();
			body = body.next;
		}
	}

	// Networking
	
	public void writeData(boolean isClient, NetworkBuffer buffer) {
//		buffer.putInt(body_count);
//		RigidBody body = body_list;
//		while(body != null) {
//			buffer.putLong(body.getId());
//			body = body.next;
//		}
	}
	
	public void readData(boolean isClient, NetworkBuffer buffer) {
//		body_count = buffer.getInt();
//		for(int i = 0; i < body_count; i++) {
//			long id = buffer.getLong();
//			RigidBody body = (RigidBody) getLevel().getComponent(id);
//			if(body == null) {
//				throw new NullPointerException();
//			}
//			if(!containsBody(body)) {
//				addBody(body);
//			}
//		}
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
