package com.spaghetti.physics;

import com.spaghetti.core.*;

/**
 * RigidBody represents the base class for any n-dimensional physics
 * implementation
 * 
 * @author bohdloss
 *
 * @param <VecType>    Children classes are required to specify their own
 *                     positional vector class
 * @param <SecVecType> Children classes are required to specify their own
 *                     rotational vector class
 */
public abstract class RigidBody<VecType, SecVecType> extends GameComponent {

	/**
	 * The BodyType is used during the initialization of a {@link RigidBody}
	 * 
	 * @author bohdloss
	 *
	 */
	public static enum BodyType {
		DYNAMIC, KINEMATIC, STATIC
	}

	// Data
	protected BodyType type;

	// Dependencies
	protected Physics physics;

	/**
	 * Create a new instance and set the body type to default
	 * ({@code BodyType.DYNAMIC})
	 */
	public RigidBody() {
		this(BodyType.DYNAMIC);
	}

	/**
	 * Create a new instance and set the body type to {@code type}
	 */
	public RigidBody(BodyType type) {
		this.type = type;
	}

	// Physics calculation

	/**
	 * Advances this body's physics simulation by a given amount of time
	 * <p>
	 * Empty implementations of this method are allowed
	 * 
	 * @param delta The time in milliseconds
	 */
	public abstract void solve(float delta);

	// Getters and setters

	// Position / rotation
	/**
	 * Retrieve the position of the body
	 * 
	 * @param pointer The vector to store the result in
	 * @return {@code pointer} if it wasn't null, a new vector otherwise
	 */
	public abstract VecType getPosition(VecType pointer);

	/**
	 * Changes the position of the body to the given parameter
	 * 
	 * @param position The new position
	 */
	public abstract void setPosition(VecType position);

	/**
	 * Retrieve the rotation of the body
	 * 
	 * @param pointer The vector to store the result in
	 * @return {@code pointer} if it wasn't null, a new vector otherwise
	 */
	public abstract SecVecType getRotation(SecVecType pointer);

	/**
	 * Changes the rotation of the body to the given parameter
	 * 
	 * @param rotation The new rotation
	 */
	public abstract void setRotation(SecVecType rotation);

	// Forces

	/**
	 * Changes the force of the body to the given parameter
	 * 
	 * @param force The new force
	 */
	public abstract void setForce(VecType force);

	/**
	 * Adds a force to the center of the body
	 * 
	 * @param force The force to apply
	 */
	public abstract void applyForce(VecType force);

	/**
	 * Adds a force to a point in the body
	 * 
	 * @param force            The force to apply
	 * @param applicationPoint The point where the force will be applied
	 */
	public abstract void applyForceAt(VecType force, VecType applicationPoint);

	/**
	 * Changes the rotational force of the body to the given parameter
	 * 
	 * @param force The new rotational force
	 */
	public abstract void setRotationForce(SecVecType force);

	/**
	 * Adds a rotational force to the body
	 * 
	 * @param force The force to apply
	 */
	public abstract void applyRotationForce(SecVecType force);

	// Velocities
	/**
	 * Retrieve the velocity of the body
	 * 
	 * @param pointer The vector to store the result in
	 * @return {@code pointer} if it wasn't null, a new vector otherwise
	 */
	public abstract VecType getVelocity(VecType pointer);

	/**
	 * Changes the velocity of the body to the given parameter
	 * 
	 * @param velocity The new velocity
	 */
	public abstract void setVelocity(VecType velocity);

	/**
	 * Adds a velocity to the center of the body
	 * 
	 * @param velocity The velocity to apply
	 */
	public abstract void applyVelocity(VecType velocity);

	/**
	 * Adds a velocity to a point in the body
	 * 
	 * @param velocity         The velocity to apply
	 * @param applicationPoint The point where the force will be applied
	 */
	public abstract void applyVelocityAt(VecType velocity, VecType applicationPoint);

	/**
	 * Retrieve the rotational velocity of the body
	 * 
	 * @param pointer The vector to store the result in
	 * @return {@code pointer} if it wasn't null, a new vector otherwise
	 */
	public abstract SecVecType getRotationVelocity(SecVecType pointer);

	/**
	 * Changes the rotational velocity of the body to the given parameter
	 * 
	 * @param velocity The new rotational velocity
	 */
	public abstract void setRotationVelocity(SecVecType velocity);

	/**
	 * Adds a rotational velocity to the body
	 * 
	 * @param velocity The velocity to apply
	 */
	public abstract void applyRotationVelocity(SecVecType velocity);

	// Other
	/**
	 * Retrieve the mass of the body
	 * 
	 * @return The mass
	 */
	public abstract float getMass();

	/**
	 * Changes the mass of the body to the given parameter
	 * 
	 * @param mass The new mass
	 */
	public abstract void setMass(float mass);

	/**
	 * Retrieve the gravity multiplier of the body
	 * 
	 * @return The gravity multiplier
	 */
	public abstract float getGravityMultiplier();

	/**
	 * Changes the gravity multiplier of the body to the given parameter
	 * 
	 * @param multiplier The new gravity multiplier
	 */
	public abstract void setGravityMultiplier(float multiplier);

	/**
	 * Retrieve the shape of the body
	 * 
	 * @param buffer The buffer to store the result in
	 * @return {@code buffer} if it wasn't null, a new shape otherwise
	 */
	public abstract Shape<VecType> getShape(Shape<VecType> buffer);

	/**
	 * Changes the shape of the body to the given parameter
	 * 
	 * @param shape The new shape
	 */
	public abstract void setShape(Shape<VecType> shape);

	/**
	 * Retrieve the friction of the body
	 * 
	 * @return The friction
	 */
	public abstract float getFriction();

	/**
	 * Changes the friction of the body to the given parameter
	 * 
	 * @param friction The new friction
	 */
	public abstract void setFriction(float friction);

	/**
	 * Retrieve the density of the body
	 * 
	 * @return The density
	 */
	public abstract float getDensity();

	/**
	 * Changes the density of the body to the given parameter
	 * 
	 * @param density The new density
	 */
	public abstract void setDensity(float density);

	/**
	 * Retrieve the restitution of the body
	 * 
	 * @return The restitution
	 */
	public abstract float getRestitution();

	/**
	 * Changes the restitution of the body to the given parameter
	 * 
	 * @param restitution The new restitution
	 */
	public abstract void setRestitution(float restitution);

	/**
	 * Retrieve whether or not the body physically reacts to collision
	 * 
	 * @return The boolean value
	 */
	public abstract boolean performsCollision();

	/**
	 * Changes whether or not the body physically reacts to collision
	 * 
	 * @param perform The boolean value
	 */
	public abstract void setPerformsCollision(boolean perform);

	/**
	 * Retrieve whether or not this body's rotation can be affected by other bodies
	 * 
	 * @return The boolean value
	 */
	public abstract boolean canRotate();

	/**
	 * Changes whether or not this body's rotation can be affected by other bodies
	 * 
	 * @param rotate The boolean value
	 */
	public abstract void setCanRotate(boolean rotate);

	/**
	 * Retrieve the linear damping of the body
	 * 
	 * @return The linear damping
	 */
	public abstract float getLinearDamping();

	/**
	 * Changes the linear damping of the body to the given parameter
	 * 
	 * @param damping The new linear damping
	 */
	public abstract void setLinearDamping(float damping);

	/**
	 * Retrieve the angular damping of the body
	 * 
	 * @return The damping
	 */
	public abstract float getAngularDamping();

	/**
	 * Changes the angular damping of the body to the given parameter
	 * 
	 * @param damping The new angular damping
	 */
	public abstract void setAngularDamping(float damping);

	// Interfaces

	/**
	 * Retrieve a reference to the physics world
	 * 
	 * @return The physics world
	 */
	public Physics getPhysics() {
		return physics;
	}

	/**
	 * Child classes must implement this method to return the class of their custom
	 * {@link Physics} implementation
	 * 
	 * @return The {@link Class} object
	 */
	protected abstract Class<? extends Physics> getPhysicsClass();

	@Override
	protected final void onBeginPlay() {
		physics = getLevel().getObject(getPhysicsClass());
		createBody();
	}

	/**
	 * Child classes may use this method to register instance of themselves in the
	 * {@link Physics} world and perform any kind of initialization
	 */
	protected abstract void createBody();

	@Override
	protected final void onEndPlay() {
		destroyBody();
	}

	/**
	 * Child classes may use this method to unregister from the {@link Physics}
	 * world and perform cleanup
	 */
	protected abstract void destroyBody();

	@Override
	protected final void commonUpdate(float delta) {
		solve(delta);
	}

}