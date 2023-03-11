package com.spaghetti.physics.d2.jbox2d;

import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.physics.RaycastRequest;
import com.spaghetti.physics.d2.Physics2D;
import com.spaghetti.physics.d2.RaycastHit2D;
import com.spaghetti.physics.d2.RaycastRequest2D;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.MathUtil;
import com.spaghetti.utils.Transform;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.joml.Vector2f;

public class JBox2DPhysics extends Physics2D {

    public static final float g = -9.81f;

    // Cache
    protected float tickAccumulator;

    // Actual world reference
    protected World world;

    public JBox2DPhysics() {
        super();
        world = new World(new Vec2(0, g));
    }

    // Physics calculation

    @Override
    public void solve(float delta) {
        // The unit of measurement is seconds
        tickAccumulator += delta;
        float frameTime = 1f / framerate;

        if (tickAccumulator < frameTime) {
            return;
        }
        tickAccumulator -= frameTime;

        // Prepare
        for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
            JBox2DRigidBody body = (JBox2DRigidBody) b.getUserData();
            body.prepare();
            body.calculateForces();
        }

        // Step
        world.step(frameTime, 6, 2);

        // Apply
        for (Body b = world.getBodyList(); b != null; b = b.getNext()) {
            JBox2DRigidBody body = (JBox2DRigidBody) b.getUserData();
            body.commit();
        }
    }

    @Override
    public void raycast(RaycastRequest<Vector2f, Float, RigidBody2D> request) {
        RaycastRequest2D r2 = (RaycastRequest2D) request;
        r2.hits.clear();
        final RayCastCallback callback = (fixture, point, normal, fraction) -> {
            RaycastHit2D hit = new RaycastHit2D();
            hit.point.x = point.x;
            hit.point.y = point.y;
            hit.normal = MathUtil.lookAt(normal.x, normal.y);
            hit.body = (RigidBody2D) fixture.getUserData();

            r2.hits.add(hit);
            return 1;
        };
        world.raycast(callback, new Vec2(r2.beginning.x, r2.beginning.y), new Vec2(r2.end.x, r2.end.y));
    }

    public void raycast(RaycastRequest2D request) {
        raycast((RaycastRequest<Vector2f, Float, RigidBody2D>) request);
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
    public void getGravity(Vector2f pointer) {
        pointer.set(world.getGravity().x, world.getGravity().y);
    }

    @Override
    public void setGravity(Vector2f vec) {
        setGravity(vec.x, vec.y);
    }

    public void setGravity(float x, float y) {
        world.setGravity(new Vec2(x, y));
    }

    // Interface implementation

    @Override
    public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
        super.writeDataServer(manager, buffer);
        buffer.putFloat(world.getGravity().x);
        buffer.putFloat(world.getGravity().y);
    }

    @Override
    public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
        super.readDataClient(manager, buffer);
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

    public World getJBox2DWorld() {
        return world;
    }

}
