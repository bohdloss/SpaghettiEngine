package com.spaghetti.physics;

import org.joml.Vector2d;
import org.joml.Vector3d;

import com.spaghetti.core.*;
import com.spaghetti.utils.CMath;

public class RigidBody extends GameComponent {

	// Dependencies

	protected Physics physics;

	// Properties

	// Shape
	protected Shape shape;

	// Coordinates
	protected Vector2d position = new Vector2d();

	// Velocity
	protected Vector2d velocity = new Vector2d();

	// Mass
	protected double mass;

	// Force
	protected Vector2d force = new Vector2d();

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
	protected Vector3d poscache = new Vector3d();
	protected Vector3d scalecache = new Vector3d();
	protected Vector3d rotcache = new Vector3d();

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

			double r = CMath.distance(position.x, position.y, b.position.x, b.position.y);
			double force_mod = physics.G * ((mass * b.mass) / (r * r));

			// Find the rotated force

			double angle = Math.atan2(b.position.y - position.y, b.position.x - position.x);

			double force_x = Math.cos(angle) * force_mod;
			double force_y = Math.sin(angle) * force_mod;

			applyForce(force_x, force_y);

		}

		// Air friction
		double friction_x = -velocity.x * physics.AIR_FRICTION;
		double friction_y = -velocity.y * physics.AIR_FRICTION;
		applyForce(friction_x, friction_y);
	}

	public void solve(double multiplier) {
		if (ignorePhysics) {
			return;
		}

		// Calculate forces
		// Such as gravity, springs, etc
		calculateForces();

		// F = m * a
		// Convert force to actual acceleration
		// a = F / m

		double xaccel = force.x / mass;
		double yaccel = force.y / mass;

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

	public void getPosition(Vector2d pointer) {
		pointer.set(position);
	}

	public void getForce(Vector2d pointer) {
		pointer.set(force);
	}

	public void getAcceleration(Vector2d pointer) {
		force.div(mass, pointer);
	}

	public void getVelocity(Vector2d pointer) {
		pointer.set(velocity);
	}

	public double getMass() {
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

	public void applyForce(double x, double y) {
		force.add(x, y);
	}

	public void applyForce(Vector2d vec) {
		applyForce(vec.x, vec.y);
	}

	public void applyAcceleration(double x, double y) {
		force.add(x * mass, y * mass);
	}

	public void applyAcceleration(Vector2d vec) {
		applyAcceleration(vec.x, vec.y);
	}

	public void applyImpulse(double x, double y) {
		velocity.add(x, y);
	}

	public void applyImpulse(Vector2d vec) {
		applyImpulse(vec.x, vec.y);
	}

	public void setPosition(double x, double y) {
		position.set(x, y);
	}

	public void setPosition(Vector2d vec) {
		setPosition(vec.x, vec.y);
	}

	public void setMass(double mass) {
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
	public void onBeginPlay() {
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
	public void onEndPlay() {
		physics.removeBody(this);
	}

}