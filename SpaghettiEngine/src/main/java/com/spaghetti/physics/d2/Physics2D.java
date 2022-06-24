package com.spaghetti.physics.d2;

import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import org.jbox2d.callbacks.RayCastCallback;
import org.joml.Vector2f;

import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.objects.Camera;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RaycastRequest;
import com.spaghetti.utils.CMath;
import com.spaghetti.utils.Transform;

public class Physics2D extends Physics {

	public static final float g = -9.81f;

	// Cache
	private float tickAccumulator;

	// Actual world reference
	protected World world;

	public Physics2D() {
		super();
		world = new World(new Vec2(0, g));
	}

	// Physics calculation

	@Override
	public void solve(float delta) {
		float tick = getGame().getTickMultiplier(delta);
		float tickTotal = tick + tickAccumulator;
		float frameTime = 1f / framerate;

		if (delta == 0 || tickTotal < frameTime) {
			tickAccumulator += tick;
			return;
		}
		tickAccumulator = 0;

		// Prepare
		for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
			RigidBody2D body = (RigidBody2D) b.getUserData();
			body.prepare();
			body.calculateForces();
		}

		// Step
		world.step(frameTime, 6, 2);

		// Apply
		for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
			RigidBody2D body = (RigidBody2D) b.getUserData();
			body.commit();
		}
	}

	@Override
	public void raycast(RaycastRequest<?, ?, ?> request) {
		RaycastRequest2D r2 = (RaycastRequest2D) request;
		r2.hits.clear();
		RayCastCallback callback = (fixture, point, normal, fraction) -> {
			RaycastHit2D hit = new RaycastHit2D();
			hit.point.x = point.x;
			hit.point.y = point.y;
			hit.normal = CMath.lookAt(normal.x, normal.y);
			hit.body = (RigidBody2D) fixture.getUserData();

			r2.hits.add(hit);
			return 1;
		};
		world.raycast(callback, new Vec2(r2.beginning.x, r2.beginning.y), new Vec2(r2.end.x, r2.end.y));
	}

	public void raycast(RaycastRequest2D request) {
		raycast((RaycastRequest<?, ?, ?>) request);
	}

	// World management

	@Override
	public int getBodyCount() {
		return world.getBodyCount();
	}

	@Override
	public RigidBody2D getBodyAt(int index) {
		int i = 0;
		for (Body body = world.getBodyList(); body != null; body = body.getNext()) {
			if (i == index) {
				return (RigidBody2D) body.getUserData();
			}
			i++;
		}
		throw new IndexOutOfBoundsException(String.valueOf(index));
	}

	// Getters and setters implementation

	@Override
	public void getGravity(Object pointer) {
		Vector2f ptr = (Vector2f) pointer;
		ptr.set(world.getGravity().x, world.getGravity().y);
	}

	@Override
	public void setGravity(Object vec) {
		Vector2f ptr = (Vector2f) vec;
		setGravity(ptr.x, ptr.y);
	}

	public void setGravity(float x, float y) {
		world.setGravity(new Vec2(x, y));
	}

	// Interface implementation

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		super.writeDataServer(buffer);
		buffer.putFloat(world.getGravity().x);
		buffer.putFloat(world.getGravity().y);
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		super.readDataClient(buffer);
		float gravity_x = buffer.getFloat();
		float gravity_y = buffer.getFloat();
		world.setGravity(new Vec2(gravity_x, gravity_y));
	}

	@Override
	protected void onBeginPlay() {
		super.onBeginPlay();
	}

	@Override
	protected void onEndPlay() {
		super.onEndPlay();
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
	}

}
