package com.spaghetti.physics;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.ToClient;

@ToClient
public abstract class RigidBody<VecType, SecVecType> extends GameComponent {

	public static enum BodyType {
		DYNAMIC, KINEMATIC, STATIC
	}

	// Data
	protected BodyType type;

	// Dependencies
	protected Physics physics;

	public RigidBody() {
		this(BodyType.DYNAMIC);
	}

	public RigidBody(BodyType type) {
		this.type = type;
	}

	// Physics calculation

	public abstract void solve(float multiplier);

	// Getters and setters

	// Position / rotation
	public abstract VecType getPosition(VecType pointer);
	public abstract void setPosition(VecType position);

	public abstract SecVecType getRotation(SecVecType pointer);
	public abstract void setRotation(SecVecType rotation);

	// Forces
	public abstract VecType getForce(VecType pointer);
	public abstract void setForce(VecType force);

	public abstract void applyForce(VecType force);
	public abstract void applyForceAt(VecType force, VecType applicationPoint);

	public abstract SecVecType getRotationForce(SecVecType pointer);

	public abstract void setRotationForce(SecVecType force);
	public abstract void applyRotationForce(SecVecType force);

	// Acceleration (forces)
	public abstract VecType getAcceleration(VecType pointer);
	public abstract void setAcceleration(VecType acceleration);

	public abstract void applyAcceleration(VecType acceleretion);
	public abstract void applyAccelerationAt(VecType acceleretion, VecType applicationPoint);

	public abstract SecVecType getRotationAcceleration(SecVecType pointer);

	public abstract void setRotationAcceleration(SecVecType acceleration);
	public abstract void applyRotationAcceleration(SecVecType acceleration);

	// Velocities
	public abstract VecType getVelocity(VecType pointer);
	public abstract void setVelocity(VecType velocity);

	public abstract void applyImpulse(VecType impulse);
	public abstract void applyImpulseAt(VecType impulse, VecType applicationPoint);

	public abstract void applyVelocity(VecType velocity);
	public abstract void applyVelocityAt(VecType velocity, VecType applicationPoint);

	public abstract SecVecType getRotationVelocity(SecVecType pointer);
	public abstract void setRotationVelocity(SecVecType velocity);

	public abstract void applyRotationImpulse(SecVecType velocity);

	public abstract void applyRotationVelocity(SecVecType velocity);

	// Other
	public abstract float getMass();
	public abstract void setMass(float mass);

	public abstract float getGravityMultiplier();
	public abstract void setGravityMultiplier(float multiplier);
	
	public abstract Shape<VecType> getShape(Shape<VecType> buffer);
	public abstract void setShape(Shape<VecType> vertices);

	public abstract float getFriction();
	public abstract void setFriction(float friction);

	public abstract float getDensity();
	public abstract void setDensity(float density);

	public abstract float getRestitution();
	public abstract void setRestitution(float restitution);

	public abstract boolean performsCollision();
	public abstract void setPerformsCollision(boolean perform);

	public abstract boolean canRotate();
	public abstract void setCanRotate(boolean rotate);

	public abstract float getLinearDamping();
	public abstract void setLinearDamping(float damping);

	public abstract float getAngularDamping();
	public abstract void setAngularDamping(float damping);

	// Interfaces

	public Physics getPhysics() {
		return physics;
	}

	protected abstract Class<? extends Physics> getPhysicsClass();

	@Override
	protected void onBeginPlay() {
		// We need a physics world!
		physics = getLevel().getObject(getPhysicsClass());
		if (physics == null) {
			try {
				physics = getPhysicsClass().newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			getLevel().addObject(physics);
		}

		createBody();
	}

	protected abstract void createBody();

	@Override
	protected void onEndPlay() {
		destroyBody();
	}

	protected abstract void destroyBody();

	@Override
	protected void commonUpdate(float delta) {
		solve(delta);
	}
	
}