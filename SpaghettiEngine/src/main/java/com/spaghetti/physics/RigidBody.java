package com.spaghetti.physics;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.spaghetti.core.*;
import com.spaghetti.utils.CMath;

public class RigidBody extends GameComponent {

	// Dependencies

	protected Physics physics;

	// Properties

	// Shape
	protected Shape shape;

	// Coordinates
	protected Vector2f position = new Vector2f();

	// Velocity
	protected Vector2f velocity = new Vector2f();

	// Mass
	protected float mass;

	// Force
	protected Vector2f force = new Vector2f();

	// Has gravity?
	protected boolean gravity = true;

	// Ignores physics?
	protected boolean ignorePhysics;

	public RigidBody() {
	}

	public RigidBody(Shape shape) {
		this.shape = shape;
	}

	// Caching data
	protected Vector3f poscache = new Vector3f();
	protected Vector3f scalecache = new Vector3f();
	protected Vector3f rotcache = new Vector3f();

	protected void gatherCache() {
		GameObject owner = getOwner();

		owner.getWorldPosition(poscache);
		owner.getWorldScale(scalecache);
		owner.getWorldRotation(rotcache);

		position.x = poscache.x;
		position.y = poscache.y;
	}

	protected void applyPosition() {
		GameObject owner = getOwner();

		owner.setWorldPosition(position.x, position.y, owner.getWorldZ());
	}

	// Collision resolving

	public boolean intersects(RigidBody other) {

		// Find rectangle intersection

		return false;

	}

	protected void calculateForces() {
		if (gravity) {
			applyAcceleration(physics.gravity);
		}
		// Just for fun
		// everything has gravity

		for (int i = 0; i < physics.body_count; i++) {
			RigidBody b = physics.body_list[i];
			if (b == this) {
				continue;
			}

			// Gravitational formula
			// F = G * ((m1 * m2) / r^2)
			// G is a constant

			float r = CMath.distance(position.x, position.y, b.position.x, b.position.y);
			float force_mod = physics.G * ((mass * b.mass) / (r * r));

			// Find the rotated force

			float angle = (float) Math.atan2(b.position.y - position.y, b.position.x - position.x);

			float force_x = (float) Math.cos(angle) * force_mod;
			float force_y = (float) Math.sin(angle) * force_mod;

			applyForce(force_x, force_y);

		}

		// Air friction
		float friction_x = -velocity.x * physics.AIR_FRICTION;
		float friction_y = -velocity.y * physics.AIR_FRICTION;
		applyForce(friction_x, friction_y);
	}

	public void solve(float multiplier) {
		if (ignorePhysics) {
			return;
		}

		// Calculate forces
		// Such as gravity, springs, etc
		calculateForces();

		// F = m * a
		// Convert force to actual acceleration
		// a = F / m

		float xaccel = force.x / mass;
		float yaccel = force.y / mass;

		// Apply the force to the velocity
		// a = (v2 - v1) / delta
		// which translates to
		// v2 = a * delta + v1

		velocity.x += xaccel * multiplier;
		velocity.y += yaccel * multiplier;

		// Apply the velocity to the space
		// same reverse formula
		// requires collision detection

		position.x += velocity.x * multiplier;
		position.y += velocity.y * multiplier;

		for (int i = 0; i < physics.body_count; i++) {
			RigidBody body = physics.body_list[i];
			if (body == this) {
				continue;
			}

			if (intersects(body)) {
			}
		}

		// Reset acceleration each frame because
		// that's how it works IRL (kind of)

		force.zero();
	}

	// Getters

	public Physics getPhysics() {
		return physics;
	}

	public void getPosition(Vector2f pointer) {
		pointer.set(position);
	}

	public void getForce(Vector2f pointer) {
		pointer.set(force);
	}

	public void getAcceleration(Vector2f pointer) {
		force.div(mass, pointer);
	}

	public void getVelocity(Vector2f pointer) {
		pointer.set(velocity);
	}

	public float getMass() {
		return mass;
	}

	public boolean getGravity() {
		return gravity;
	}

	public boolean getIgnorePhysics() {
		return ignorePhysics;
	}

	public Shape getShape() {
		return shape;
	}

	// Setters

	public void applyForce(float x, float y) {
		force.add(x, y);
	}

	public void applyForce(Vector2f vec) {
		applyForce(vec.x, vec.y);
	}

	public void applyAcceleration(float x, float y) {
		force.add(x * mass, y * mass);
	}

	public void applyAcceleration(Vector2f vec) {
		applyAcceleration(vec.x, vec.y);
	}

	public void applyImpulse(float x, float y) {
		velocity.add(x, y);
	}

	public void applyImpulse(Vector2f vec) {
		applyImpulse(vec.x, vec.y);
	}

	public void setPosition(float x, float y) {
		position.set(x, y);
	}

	public void setPosition(Vector2f vec) {
		setPosition(vec.x, vec.y);
	}

	public void setMass(float mass) {
		this.mass = mass;
	}

	public void setGravity(boolean gravity) {
		this.gravity = gravity;
	}

	public void setIgnorePhysics(boolean ignorePhysics) {
		this.ignorePhysics = ignorePhysics;
	}

	public void setShape(Shape shape) {
		this.shape = shape;
	}

	// Interfaces

	@Override
	protected void onBeginPlay() {
		// We need a physics world!
		physics = getLevel().getObject(Physics.class);
		if (physics == null) {
			physics = new Physics();
			getLevel().addObject(physics);
		}
		if (mass == 0) {
			mass = 10;
		}
		physics.addBody(this);

		// Set position based on owner
		GameObject obj = getOwner();
		position.x = obj.getRelativeX();
		position.y = obj.getRelativeY();

	}

	@Override
	protected void onEndPlay() {
		physics.removeBody(this);
	}

}