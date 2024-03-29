package com.spaghetti.physics.d2.jbox2d;

import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.d2.Physics2D;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.physics.d2.Shape2D;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.MathUtil;
import com.spaghetti.utils.Transform;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.MassData;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.*;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class JBox2DRigidBody extends RigidBody2D {

    // Temporary
    protected Vector2f position = new Vector2f();
    protected float angle = 0;
    protected Vector2f linearForce = new Vector2f();
    protected float angularForce = 0;
    protected Vector2f linearVelocity = new Vector2f();
    protected float angularVelocity = 0;

    protected float mass = 1;
    protected float gravityMultiplier = 1;
    protected Shape2D shape;
    protected float friction = 0;
    protected float density = 1;
    protected float restitution = 0;
    protected boolean performCollision = true;
    protected boolean canRotate = true;
    protected float linearDamping = 0;
    protected float angularDamping = 0;

    // Actual body
    protected Body body;

    // Cache
    protected boolean initShape = false;
    protected Vector2f last_scale = new Vector2f(1, 1);

    public JBox2DRigidBody() {
        this(BodyType.DYNAMIC);
    }

    public JBox2DRigidBody(BodyType type) {
        super(type);
        this.shape = new Shape2D();
        this.shape.addVertices(new Vector2f[] {new Vector2f(-0.5f, 0.5f),
                new Vector2f(0.5f, 0.5f),
                new Vector2f(0.5f, -0.5f),
                new Vector2f(-0.5f, -0.5f)});
    }

    // Internal utility

    protected FixtureDef copyFixture() {
        Fixture original = body.getFixtureList();
        FixtureDef result = new FixtureDef();
        result.density = original.getDensity();
        result.filter = original.getFilterData();
        result.friction = original.getFriction();
        result.isSensor = original.isSensor();
        result.restitution = original.getRestitution();
        result.shape = original.getShape();
        result.userData = original.getUserData();

        return result;
    }

    protected void swapFixture(FixtureDef fixture) {
        body.destroyFixture(body.getFixtureList());
        body.createFixture(fixture);
    }

    // Physics calculation implementation

    public void prepare() {
        Vector3f ptr = new Vector3f();

        // Apply scale
        getOwner().getWorldScale(ptr);
        if (!MathUtil.equals(ptr.x, last_scale.x, 0.001f) || !MathUtil.equals(ptr.y, last_scale.y, 0.001f) || !initShape) {
            // The scale changed since the last time
            float diffx = ptr.x / last_scale.x;
            float diffy = ptr.y / last_scale.y;

            last_scale.x = ptr.x;
            last_scale.y = ptr.y;

            // Redefine shape
            Shape2D shape = getShape(null);
            if (!shape.isCircle()) {
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    Vector2f vertex = shape.getVertex(i);
                    if(diffx != 0) {
                        vertex.x *= diffx;
                    }
                    if(diffy != 0) {
                        vertex.y *= diffy;
                    }
                }
            }

            setShape(shape);
            initShape = true;
        }

        // Apply change in position
        getOwner().getWorldPosition(ptr);
        body.setTransform(new Vec2(ptr.x, ptr.y), getOwner().getRoll());
    }

    public void commit() {
        Vector3f ptr = new Vector3f();

        // Apply position
        ptr.x = body.getPosition().x;
        ptr.y = body.getPosition().y;
        ptr.z = getOwner().getWorldZ();
        getOwner().setWorldPosition(ptr);

        // Apply rotation
        getOwner().setWorldRoll(body.getAngle());
    }

    protected void calculateForces() {
    }

    @Override
    public void solve(float multiplier) {
        // NOTHING
        // Physics2D takes care of that
    }

    // Position / rotation

    @Override
    public Vector2f getPosition(Vector2f pointer) {
        if (pointer == null) {
            pointer = new Vector2f();
        }

        if (body == null) {
            pointer.set(position);
        } else {
            pointer.x = body.getPosition().x;
            pointer.y = body.getPosition().y;
        }
        return pointer;
    }

    public void setPosition(float x, float y) {
        if (body == null) {
            this.position.set(x, y);
        } else {
            body.setTransform(new Vec2(x, y), body.getAngle());
            body.setAwake(true);
        }
    }

    protected void setPositionNoAwake(float x, float y) {
        if (body == null) {
            this.position.set(x, y);
        } else {
            body.setTransform(new Vec2(x, y), body.getAngle());
        }
    }

    @Override
    public float getRotation() {
        if (body == null) {
            return angle;
        } else {
            return body.getAngle();
        }
    }

    @Override
    public void setRotation(Float rotation) {
        if (body == null) {
            this.angle = rotation;
        } else {
            body.setTransform(body.getPosition(), rotation);
            body.setAwake(true);
        }
    }

    protected void setRotationNoAwake(float rotation) {
        if (body == null) {
            this.angle = rotation;
        } else {
            body.setTransform(body.getPosition(), rotation);
        }
    }

    // Forces

    public void setForce(float x, float y) {
        if (body == null) {
            this.linearForce.set(x, y);
        } else {
            body.m_force.x = x;
            body.m_force.y = y;
        }
    }

    @Override
    public void applyForce(float x, float y) {
        if (body == null) {
            this.linearForce.add(x, y);
        } else {
            body.applyForceToCenter(new Vec2(x, y));
        }
    }

    @Override
    public void applyForceAt(float forceX, float forceY, float applicationX, float applicationY) {
        if (body == null) {
            this.linearForce.add(forceX, forceY);
            this.angularForce += applicationX * forceY - applicationY * forceX;
        } else {
            body.applyForce(new Vec2(forceX, forceY), new Vec2(applicationX, applicationY));
        }
    }

    @Override
    public void setRotationForce(Float force) {
        if (body == null) {
            this.angularForce = force;
        } else {
            body.m_torque = force;
        }
    }

    @Override
    public void applyRotationForce(Float force) {
        if (body == null) {
            this.angularForce += force;
        } else {
            body.applyTorque(force);
        }
    }

    // Velocity

    @Override
    public Vector2f getVelocity(Vector2f pointer) {
        if (pointer == null) {
            pointer = new Vector2f();
        }

        if (body == null) {
            pointer.set(linearVelocity);
        } else {
            pointer.x = body.getLinearVelocity().x;
            pointer.y = body.getLinearVelocity().y;
        }
        return pointer;
    }

    @Override
    public void setVelocity(float x, float y) {
        if (body == null) {
            this.linearVelocity.set(x, y);
        } else {
            body.setLinearVelocity(new Vec2(x, y));
        }
    }

    @Override
    public void applyVelocity(float x, float y) {
        if (body == null) {
            this.linearVelocity.add(x, y);
        } else {
            MassData data = new MassData();
            body.getMassData(data);
            body.applyLinearImpulse(new Vec2(x * body.getMass(), y * body.getMass()),
                    new Vec2(data.center.x + body.getPosition().x, data.center.y + body.getPosition().y));
        }
    }

    @Override
    public void applyVelocityAt(float x, float y, float applicationX, float applicationY) {
        if (body == null) {
            this.linearVelocity.add(x, y);
            this.angularVelocity += applicationX * y - applicationY * x;
        } else {
            body.applyLinearImpulse(new Vec2(x * body.getMass(), y * body.getMass()),
                    new Vec2(applicationX, applicationY));
        }
    }

    @Override
    public float getRotationVelocity() {
        if (body == null) {
            return angularVelocity;
        } else {
            return body.getAngularVelocity();
        }
    }

    @Override
    public void setRotationVelocity(Float velocity) {
        if (body == null) {
            this.angularVelocity = velocity;
        } else {
            body.setAngularVelocity(velocity);
        }
    }

    @Override
    public void applyRotationVelocity(Float velocity) {
        if (body == null) {
            this.angularVelocity += velocity;
        } else {
            body.applyAngularImpulse(velocity * body.getMass());
        }
    }

    // Other

    @Override
    public float getMass() {
        if (body == null) {
            return mass;
        } else {
            return body.getMass();
        }
    }

    @Override
    public void setMass(float mass) {
        if (body == null) {
            this.mass = mass;
        } else {
            MassData data = new MassData();
            body.getMassData(data);

            data.mass = mass;

            body.setMassData(data);
        }
    }

    @Override
    public float getGravityMultiplier() {
        if (body == null) {
            return gravityMultiplier;
        } else {
            return body.getGravityScale();
        }
    }

    @Override
    public void setGravityMultiplier(float multiplier) {
        if (body == null) {
            this.gravityMultiplier = multiplier;
        } else {
            body.setGravityScale(multiplier);
        }
    }

    @Override
    public Shape2D getShape(com.spaghetti.physics.Shape<Vector2f> buffer) {
        if (buffer == null) {
            buffer = new Shape2D();
        }

        Shape2D _buffer = (Shape2D) buffer;
        _buffer.clear();

        if (body == null) {
            _buffer.set(shape);
        } else {
            Shape bodyShape = body.getFixtureList().getShape();

            switch (bodyShape.m_type) {
                case POLYGON:
                    // Translate box2d's Vec2 types back to Vector2f
                    PolygonShape pShape = (PolygonShape) bodyShape;
                    Vec2[] vertices = pShape.m_vertices;
                    for (Vec2 vertex : vertices) {
                        _buffer.addVertex(new Vector2f(vertex.x, vertex.y));
                    }
                    break;

                case CIRCLE:
                    // Simply copy the radius
                    CircleShape cShape = (CircleShape) bodyShape;
                    _buffer.setRadius(cShape.getRadius());
                    break;
                default:
                    _buffer.clear();
            }
        }

        return _buffer;
    }

    @Override
    public void setShape(com.spaghetti.physics.Shape<Vector2f> shape) {
        if (body == null) {
            this.shape.set(shape);
        } else {
            FixtureDef copy = copyFixture();

            Shape result;
            if (shape.isCircle()) {
                // Initialize a circle shape
                CircleShape cshape = new CircleShape();
                cshape.setRadius(shape.getRadius());

                // Apply the results
                result = cshape;
            } else {
                // Initialize a polygon shape
                PolygonShape pshape = new PolygonShape();

                // Copy everything to box2d's Vec2 type
                Vec2[] secondary = new Vec2[shape.getVertexCount()];
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    Vector2f shape_vertex = shape.getVertex(i);
                    secondary[i] = new Vec2(shape_vertex.x, shape_vertex.y);
                }

                // Apply the results
                pshape.set(secondary, secondary.length);
                result = pshape;
            }
            copy.shape = result;

            swapFixture(copy);
        }
    }

    @Override
    public float getFriction() {
        if (body == null) {
            return friction;
        } else {
            return body.getFixtureList().getFriction();
        }
    }

    @Override
    public void setFriction(float friction) {
        if (body == null) {
            this.friction = friction;
        } else {
            body.getFixtureList().setFriction(friction);
        }
    }

    @Override
    public float getDensity() {
        if (body == null) {
            return density;
        } else {
            return body.getFixtureList().getDensity();
        }
    }

    @Override
    public void setDensity(float density) {
        if (body == null) {
            this.density = density;
        } else {
            body.getFixtureList().setDensity(density);
        }
    }

    @Override
    public float getRestitution() {
        if (body == null) {
            return restitution;
        } else {
            return body.getFixtureList().getRestitution();
        }
    }

    @Override
    public void setRestitution(float restitution) {
        if (body == null) {
            this.restitution = restitution;
        } else {
            body.getFixtureList().setRestitution(restitution);
        }
    }

    @Override
    public boolean performsCollision() {
        if (body == null) {
            return performCollision;
        } else {
            return !body.getFixtureList().isSensor();
        }
    }

    @Override
    public void setPerformsCollision(boolean perform) {
        if (body == null) {
            this.performCollision = perform;
        } else {
            body.getFixtureList().setSensor(!perform);
        }
    }

    @Override
    public boolean canRotate() {
        if (body == null) {
            return canRotate;
        } else {
            return !body.isFixedRotation();
        }
    }

    @Override
    public void setCanRotate(boolean rotate) {
        if (body == null) {
            this.canRotate = rotate;
        } else {
            MassData data = new MassData();
            body.getMassData(data);
            body.setFixedRotation(!rotate);
            body.setMassData(data);
        }
    }

    @Override
    public float getLinearDamping() {
        if (body == null) {
            return linearDamping;
        } else {
            return body.getLinearDamping();
        }
    }

    @Override
    public void setLinearDamping(float damping) {
        if (body == null) {
            this.linearDamping = damping;
        } else {
            body.setLinearDamping(damping);
        }
    }

    @Override
    public float getAngularDamping() {
        if (body == null) {
            return angularDamping;
        } else {
            return body.getAngularDamping();
        }
    }

    @Override
    public void setAngularDamping(float damping) {
        if (body == null) {
            this.angularDamping = damping;
        } else {
            body.setAngularDamping(damping);
        }
    }

    // Interface implementation

    @Override
    public JBox2DPhysics getPhysics() {
        return (JBox2DPhysics) super.getPhysics();
    }

    @Override
    protected Class<? extends Physics> getPhysicsClass() {
        return JBox2DPhysics.class;
    }

    @Override
    protected void createBody() {
        // Define body
        BodyDef def = new BodyDef();
        def.active = true;
        def.awake = true;
        def.position = new Vec2(0, 0);

        switch (this.type) {
            case DYNAMIC:
                def.type = org.jbox2d.dynamics.BodyType.DYNAMIC;
                break;
            case KINEMATIC:
                def.type = org.jbox2d.dynamics.BodyType.KINEMATIC;
                break;
            case STATIC:
                def.type = org.jbox2d.dynamics.BodyType.STATIC;
                break;
        }

        // Create body
        body = getPhysics().getJBox2DWorld().createBody(def);

        // Define shape
        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.5f, 0.5f);

        // Define fixture
        FixtureDef fixture = new FixtureDef();
        fixture.shape = shape;
        fixture.userData = this;

        body.createFixture(fixture);
        body.setUserData(this);

        Vector3f vec = new Vector3f();

        getOwner().getWorldPosition(vec);
        setPosition(vec.x, vec.y);

        setRotation(getOwner().getWorldRoll());

        if (this.shape.isValid()) {
            setShape(this.shape);
        }
        setForce(linearForce);
        setRotationForce(angularForce);
        setVelocity(linearVelocity);
        setRotationVelocity(angularVelocity);
        setMass(mass);
        setGravityMultiplier(gravityMultiplier);
        setFriction(friction);
        setDensity(density);
        setRestitution(restitution);
        setPerformsCollision(performCollision);
        setCanRotate(canRotate);
        setLinearDamping(linearDamping);
        setAngularDamping(angularDamping);
    }

    @Override
    protected void destroyBody() {
        getPosition(position);
        angle = getRotation();
        linearForce.set(body.m_force.x, body.m_force.y);
        angularForce = body.m_torque;
        getVelocity(linearVelocity);
        angularVelocity = getRotationVelocity();
        mass = getMass();
        gravityMultiplier = getGravityMultiplier();
        getShape(shape);
        friction = getFriction();
        density = getDensity();
        restitution = getRestitution();
        performCollision = performsCollision();
        canRotate = canRotate();
        linearDamping = getLinearDamping();
        angularDamping = getAngularDamping();

        JBox2DPhysics physics = (JBox2DPhysics) getPhysics();
        World world = physics.getJBox2DWorld();
        world.destroyBody(body);
        body = null;
    }

    @Override
    public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
        super.writeDataServer(manager, buffer);
        buffer.putFloat(body.getPosition().x);
        buffer.putFloat(body.getPosition().y);
        buffer.putFloat(body.getAngle());
        buffer.putFloat(body.getLinearVelocity().x);
        buffer.putFloat(body.getLinearVelocity().x);
        buffer.putFloat(body.getAngularVelocity());
    }

    @Override
    public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
        super.readDataClient(manager, buffer);
        float posx = buffer.getFloat();
        float posy = buffer.getFloat();
        float posang = buffer.getFloat();
        float velx = buffer.getFloat();
        float vely = buffer.getFloat();
        float velang = buffer.getFloat();

        this.setPositionNoAwake(posx, posy);
        this.setRotationNoAwake(posang);
        this.setVelocity(velx, vely);
        this.setRotationVelocity(velang);

    }

    @Override
    public boolean needsReplication(ConnectionManager manager) {
        return false;
    }

    @Override
    public void render(Camera renderer, float delta, Transform transform) {
    }

}
