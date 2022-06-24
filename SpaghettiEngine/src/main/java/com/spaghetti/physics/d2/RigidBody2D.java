package com.spaghetti.physics.d2;

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
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.objects.Camera;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;
import com.spaghetti.utils.Transform;

public class RigidBody2D extends RigidBody<Vector2f, Float> {

	// Temporary
	protected Vector2f position = new Vector2f();
	protected float angle = 0;
	protected Vector2f linearForce = new Vector2f();
	protected float angularForce = 0;
	protected Vector2f linearVelocity = new Vector2f();
	protected float angularVelocity = 0;

	protected float mass = 1;
	protected float gravityMultiplier = 1;
	protected Shape2D shape = new Shape2D();
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
	protected Vector2f last_scale = new Vector2f(1, 1);

	public RigidBody2D() {
		this(BodyType.DYNAMIC);

	}

	public RigidBody2D(BodyType type) {
		super(type);
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

	protected void prepare() {
		Vector3f ptr = new Vector3f();
		getOwner().getWorldScale(ptr);
		if (!CMath.equals(ptr.x, last_scale.x, 0.001f) || !CMath.equals(ptr.y, last_scale.y, 0.001f)) {
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
					vertex.x *= diffx;
					vertex.y *= diffy;
				}
			}

			setShape(shape);
		}
	}

	protected void commit() {
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

	@Override
	public void setPosition(Vector2f position) {
		setPosition(position.x, position.y);
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
	public Float getRotation(Float __null__) {
		return getRotation();
	}

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

	@Override
	public void setForce(Vector2f force) {
		setForce(force.x, force.y);
	}

	public void setForce(float x, float y) {
		if (body == null) {
			this.linearForce.set(x, y);
		} else {
			body.m_force.x = x;
			body.m_force.y = y;
		}
	}

	@Override
	public void applyForce(Vector2f force) {
		applyForce(force.x, force.y);
	}

	public void applyForce(float x, float y) {
		if (body == null) {
			this.linearForce.add(x, y);
		} else {
			body.applyForceToCenter(new Vec2(x, y));
		}
	}

	@Override
	public void applyForceAt(Vector2f force, Vector2f applicationPoint) {
		applyForceAt(force.x, force.y, applicationPoint.x, applicationPoint.y);
	}

	public void applyForceAt(float x, float y, float applicationX, float applicationY) {
		if (body == null) {
			this.linearForce.add(x, y);
			this.angularForce += applicationX * y - applicationY * x;
		} else {
			body.applyForce(new Vec2(x, y), new Vec2(applicationX, applicationY));
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
	public void setVelocity(Vector2f velocity) {
		setVelocity(velocity.x, velocity.y);
	}

	public void setVelocity(float x, float y) {
		if (body == null) {
			this.linearVelocity.set(x, y);
		} else {
			body.setLinearVelocity(new Vec2(x, y));
		}
	}

	@Override
	public void applyVelocity(Vector2f velocity) {
		applyVelocity(velocity.x, velocity.y);
	}

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
	public void applyVelocityAt(Vector2f velocity, Vector2f applicationPoint) {
		applyVelocityAt(velocity.x, velocity.y, applicationPoint.x, applicationPoint.y);
	}

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
	public Float getRotationVelocity(Float __null__) {
		return getRotationVelocity();
	}

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
				PolygonShape pshape = (PolygonShape) bodyShape;
				Vec2[] vertices = pshape.m_vertices;
				for (Vec2 vertex : vertices) {
					_buffer.addVertex(new Vector2f(vertex.x, vertex.y));
				}
				break;

			case CIRCLE:
				// Simply copy the radius
				CircleShape cshape = (CircleShape) bodyShape;
				_buffer.setRadius(cshape.getRadius());
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
	public Physics2D getPhysics() {
		return (Physics2D) super.getPhysics();
	}

	@Override
	protected Class<? extends Physics> getPhysicsClass() {
		return Physics2D.class;
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
		body = getPhysics().world.createBody(def);

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

		getOwner().getWorldRotation(vec);
		setRotation(vec.z);

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

		Physics2D physics = getPhysics();
		World world = physics.world;
		world.destroyBody(body);
		body = null;
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		super.writeDataServer(buffer);
		buffer.putFloat(body.getPosition().x);
		buffer.putFloat(body.getPosition().y);
		buffer.putFloat(body.getAngle());
		buffer.putFloat(body.getLinearVelocity().x);
		buffer.putFloat(body.getLinearVelocity().x);
		buffer.putFloat(body.getAngularVelocity());
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		super.readDataClient(buffer);
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
	public boolean needsReplication(ConnectionManager client) {
		return false;
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
	}

}
