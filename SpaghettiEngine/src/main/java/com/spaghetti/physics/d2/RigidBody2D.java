package com.spaghetti.physics.d2;

import com.spaghetti.physics.d2.jbox2d.JBox2DPhysics;
import com.spaghetti.utils.GameSettings;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;
import org.joml.Vector3f;

import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.render.Camera;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.MathUtil;
import com.spaghetti.utils.Transform;

import java.lang.reflect.InvocationTargetException;

public abstract class RigidBody2D extends RigidBody<Vector2f, Float> {

	private static Class<? extends RigidBody2D> bodyClass;
	private static String bodyClassName;

	private static void updateClass() {
		try {
			// Change in body class, update class
			String setting = GameSettings.sgetEngineSetting("physics.d2.rigidBodyClass");
			if (!setting.equals(bodyClassName)) {
				bodyClassName = setting;
				bodyClass = (Class<? extends RigidBody2D>) Class.forName(bodyClassName);
			}
		} catch(ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static RigidBody2D getInstance(BodyType type) {
		try {
			updateClass();

			// Instantiate body and return
			return bodyClass.getConstructor(BodyType.class).newInstance();
		} catch(NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	public static RigidBody2D getInstance() {
		try {
			updateClass();

			// Instantiate body and return
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
