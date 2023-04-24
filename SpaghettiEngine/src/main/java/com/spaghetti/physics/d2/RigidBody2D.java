package com.spaghetti.physics.d2;

import com.spaghetti.utils.settings.GameSettings;
import org.joml.Vector2f;

import com.spaghetti.physics.RigidBody;

import java.lang.reflect.InvocationTargetException;

public abstract class RigidBody2D extends RigidBody<Vector2f, Float> {

	public static RigidBody2D getInstance(BodyType type) {
		try {
			Class<? extends RigidBody2D> bodyClass = GameSettings.sgetEngineSetting("physics.d2.rigidBodyClass");
			return bodyClass.getConstructor(BodyType.class).newInstance(type);
		} catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static RigidBody2D getInstance() {
		try {
			Class<? extends RigidBody2D> bodyClass = GameSettings.sgetEngineSetting("physics.d2.rigidBodyClass");
			return bodyClass.getConstructor().newInstance();
		} catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public RigidBody2D() {
		super();
	}

	public RigidBody2D(BodyType type) {
		super(type);
	}

	public void setPosition(Vector2f vector) {
		setPosition(vector.x, vector.y);
	}

	public abstract void setPosition(float x, float y);

	@Override
	public Float getRotation(Float __null__) {
		return getRotation();
	}

	public abstract float getRotation();

	@Override
	public void setForce(Vector2f force) {
		setForce(force.x, force.y);
	}

	public abstract void setForce(float x, float y);

	@Override
	public void applyForce(Vector2f force) {
		applyForce(force.x, force.y);
	}

	public abstract void applyForce(float x, float y);

	@Override
	public void applyForceAt(Vector2f force, Vector2f applicationPoint) {
		applyForceAt(force.x, force.y, applicationPoint.x, applicationPoint.y);
	}

	public abstract void applyForceAt(float forceX, float forceY, float applicationX, float applicationY);

	@Override
	public void setVelocity(Vector2f velocity) {
		setVelocity(velocity.x, velocity.y);
	}

	public abstract void setVelocity(float x, float y);

	@Override
	public void applyVelocity(Vector2f velocity) {
		applyVelocity(velocity.x, velocity.y);
	}

	public abstract void applyVelocity(float x, float y);

	@Override
	public void applyVelocityAt(Vector2f velocity, Vector2f applicationPoint) {
		applyVelocityAt(velocity.x, velocity.y, applicationPoint.x, applicationPoint.y);
	}

	public abstract void applyVelocityAt(float velocityX, float velocityY, float applicationX, float applicationY);

	@Override
	public Float getRotationVelocity(Float __null__) {
		return getRotationVelocity();
	}

	public abstract float getRotationVelocity();

}
