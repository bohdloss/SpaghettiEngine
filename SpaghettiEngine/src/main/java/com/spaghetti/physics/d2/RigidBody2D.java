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
import com.spaghetti.interfaces.ToClient;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.objects.Camera;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;
import com.spaghetti.utils.Utils;

@ToClient
public class RigidBody2D extends RigidBody<Vector2f, Float> {

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
			if(!shape.isCircle()) {
				for(int i = 0; i < shape.getVertexCount(); i++) {
					Vector2f vertex = shape.getVertex(i);
					vertex.x *= diffx;
					vertex.y *= diffy;
				}
			}
			
			setShape(shape);
		}
	}

	protected void applyPosition() {
		Vector3f ptr = new Vector3f();

		// Apply position
		ptr.x = body.getPosition().x;
		ptr.y = body.getPosition().y;
		ptr.z = getOwner().getWorldZ();
		getOwner().setWorldPosition(ptr);

		// Apply rotation
		getOwner().getWorldRotation(ptr);
		ptr.z = body.getAngle();
		getOwner().setWorldRotation(ptr);
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
		if(pointer == null) {
			pointer = new Vector2f();
		}
		pointer.x = body.getPosition().x;
		pointer.y = body.getPosition().y;
		return pointer;
	}

	@Override
	public void setPosition(Vector2f position) {
		setPosition(position.x, position.y);
	}

	public void setPosition(float x, float y) {
		body.setTransform(new Vec2(x, y), body.getAngle());
		body.setAwake(true);
	}

	@Override
	public Float getRotation(Float __null__) {
		return body.getAngle();
	}

	public float getRotation() {
		return body.getAngle();
	}

	@Override
	public void setRotation(Float rotation) {
		body.setTransform(body.getPosition(), rotation);
	}

	// Forces

	@Override
	public Vector2f getForce(Vector2f pointer) {
		if(pointer == null) {
			pointer = new Vector2f();
		}
		pointer.x = body.m_force.x;
		pointer.y = body.m_force.y;
		return pointer;
	}

	@Override
	public void setForce(Vector2f force) {
		setForce(force.x, force.y);
	}

	public void setForce(float x, float y) {
		body.m_force.x = x;
		body.m_force.y = y;
	}

	@Override
	public void applyForce(Vector2f force) {
		applyForce(force.x, force.y);
	}

	public void applyForce(float x, float y) {
		body.applyForceToCenter(new Vec2(x, y));
	}

	@Override
	public void applyForceAt(Vector2f force, Vector2f applicationPoint) {
		applyForceAt(force.x, force.y, applicationPoint.x, applicationPoint.y);
	}

	public void applyForceAt(float x, float y, float applicationX, float applicationY) {
		body.applyForce(new Vec2(x, y), new Vec2(applicationX, applicationY));
	}

	@Override
	public Float getRotationForce(Float __null__) {
		return body.m_torque;
	}

	public float getRotationForce() {
		return body.m_torque;
	}

	@Override
	public void setRotationForce(Float force) {
		body.m_torque = force;
	}

	@Override
	public void applyRotationForce(Float force) {
		body.applyTorque(force);
	}

	// Acceleration (forces)

	@Override
	public Vector2f getAcceleration(Vector2f pointer) {
		if(pointer == null) {
			pointer = new Vector2f();
		}
		pointer.x = body.m_force.x / body.getMass();
		pointer.y = body.m_force.y / body.getMass();
		return pointer;
	}

	@Override
	public void setAcceleration(Vector2f acceleration) {
		setAcceleration(acceleration.x, acceleration.y);
	}

	public void setAcceleration(float x, float y) {
		body.m_force.x = x * body.getMass();
		body.m_force.y = y * body.getMass();
	}

	@Override
	public void applyAcceleration(Vector2f acceleration) {
		applyAcceleration(acceleration.x, acceleration.y);
	}

	public void applyAcceleration(float x, float y) {
		body.applyForceToCenter(new Vec2(x * body.getMass(), y * body.getMass()));
	}

	@Override
	public void applyAccelerationAt(Vector2f acceleration, Vector2f applicationPoint) {
		applyAccelerationAt(acceleration.x, acceleration.y, applicationPoint.x, applicationPoint.y);
	}

	public void applyAccelerationAt(float x, float y, float applicationX, float applicationY) {
		body.applyForce(new Vec2(x, y), new Vec2(applicationX, applicationY));
	}

	@Override
	public Float getRotationAcceleration(Float __null__) {
		return body.m_torque / body.getMass();
	}

	public float getRotationAcceleration() {
		return body.m_torque / body.getMass();
	}

	@Override
	public void setRotationAcceleration(Float acceleration) {
		body.m_torque = acceleration * body.getMass();
	}

	@Override
	public void applyRotationAcceleration(Float acceleration) {
		body.applyTorque(acceleration * body.getMass());
	}

	// Velocity

	@Override
	public Vector2f getVelocity(Vector2f pointer) {
		if(pointer == null) {
			pointer = new Vector2f();
		}
		pointer.x = body.getLinearVelocity().x;
		pointer.y = body.getLinearVelocity().y;
		return pointer;
	}

	@Override
	public void setVelocity(Vector2f velocity) {
		setVelocity(velocity.x, velocity.y);
	}

	public void setVelocity(float x, float y) {
		body.setLinearVelocity(new Vec2(x, y));
	}

	@Override
	public void applyImpulse(Vector2f impulse) {
		applyImpulse(impulse.x, impulse.y);
	}

	public void applyImpulse(float x, float y) {
		MassData data = new MassData();
		body.getMassData(data);
		applyImpulseAt(x, y, data.center.x + body.getPosition().x, data.center.y + body.getPosition().y);
	}

	@Override
	public void applyImpulseAt(Vector2f impulse, Vector2f applicationPoint) {
		applyImpulseAt(impulse.x, impulse.y, applicationPoint.x, applicationPoint.y);
	}

	public void applyImpulseAt(float x, float y, float applicationX, float applicationY) {
		body.applyLinearImpulse(new Vec2(x, y), new Vec2(applicationX, applicationY));
	}

	@Override
	public void applyVelocity(Vector2f velocity) {
		applyVelocity(velocity.x, velocity.y);
	}

	public void applyVelocity(float x, float y) {
		applyImpulse(x * body.getMass(), y * body.getMass());
	}

	@Override
	public void applyVelocityAt(Vector2f velocity, Vector2f applicationPoint) {
		applyVelocityAt(velocity.x, velocity.y, applicationPoint.x, applicationPoint.y);
	}

	public void applyVelocityAt(float x, float y, float applicationX, float applicationY) {
		applyImpulseAt(x * body.getMass(), y * body.getMass(), applicationX, applicationY);
	}

	@Override
	public Float getRotationVelocity(Float __null__) {
		return body.getAngularVelocity();
	}

	public float getRotationVelocity() {
		return body.getAngularVelocity();
	}

	@Override
	public void setRotationVelocity(Float velocity) {
		body.setAngularVelocity(velocity);
	}

	@Override
	public void applyRotationImpulse(Float impulse) {
		body.applyAngularImpulse(impulse);
	}

	@Override
	public void applyRotationVelocity(Float velocity) {
		body.applyAngularImpulse(velocity * body.getMass());
	}

	// Other

	@Override
	public float getMass() {
		return body.getMass();
	}

	@Override
	public void setMass(float mass) {
		MassData data = new MassData();
		body.getMassData(data);

		data.mass = mass;

		body.setMassData(data);
	}

	@Override
	public float getGravityMultiplier() {
		return body.getGravityScale();
	}

	@Override
	public void setGravityMultiplier(float multiplier) {
		body.setGravityScale(multiplier);
	}

	@Override
	public Shape2D getShape(com.spaghetti.physics.Shape<Vector2f> buffer) {
		if(buffer == null) {
			buffer = new Shape2D();
		}
		
		Shape2D _buffer = (Shape2D) buffer;
		_buffer.clear();
		Shape shape = body.getFixtureList().getShape();
		
		switch(shape.m_type) {
		case POLYGON:
			// Translate box2d's Vec2 types back to Vector2f
			PolygonShape pshape = (PolygonShape) shape;
			Vec2[] vertices = pshape.m_vertices;
			for(Vec2 vertex : vertices) {
				_buffer.addVertex(new Vector2f(vertex.x, vertex.y));
			}
			break;
			
		case CIRCLE:
			// Simply copy the radius
			CircleShape cshape = (CircleShape) shape;
			_buffer.setRadius(cshape.getRadius());
			break;
			
		default:
			return null; // This should not happen and is most definitely a bug
		}
		
		return _buffer;
	}
	
	@Override
	public void setShape(com.spaghetti.physics.Shape<Vector2f> shape) {
		FixtureDef copy = copyFixture();

		Shape result;
		if(shape.isCircle()) {
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

	@Override
	public float getFriction() {
		return body.getFixtureList().getFriction();
	}

	@Override
	public void setFriction(float friction) {
		body.getFixtureList().setFriction(friction);
	}

	@Override
	public float getDensity() {
		return body.getFixtureList().getDensity();
	}

	@Override
	public void setDensity(float density) {
		body.getFixtureList().setDensity(density);
	}

	@Override
	public float getRestitution() {
		return body.getFixtureList().getRestitution();
	}

	@Override
	public void setRestitution(float restitution) {
		body.getFixtureList().setRestitution(restitution);
	}

	@Override
	public boolean performsCollision() {
		return !body.getFixtureList().isSensor();
	}

	@Override
	public void setPerformsCollision(boolean perform) {
		body.getFixtureList().setSensor(!perform);
	}

	@Override
	public boolean canRotate() {
		return !body.isFixedRotation();
	}

	@Override
	public void setCanRotate(boolean rotate) {
		MassData data = new MassData();
		body.getMassData(data);
		body.setFixedRotation(!rotate);
		body.setMassData(data);
	}

	@Override
	public float getLinearDamping() {
		return body.getLinearDamping();
	}

	@Override
	public void setLinearDamping(float damping) {
		body.setLinearDamping(damping);
	}

	@Override
	public float getAngularDamping() {
		return body.getAngularDamping();
	}

	@Override
	public void setAngularDamping(float damping) {
		body.setAngularDamping(damping);
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
		Utils.printStackTrace();

		// Define shape
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(0.5f, 0.5f);

		// Define fixture
		FixtureDef fixture = new FixtureDef();
		fixture.shape = shape;
		fixture.friction = 0.3f;
		fixture.density = 1f;
		fixture.userData = this;

		body.createFixture(fixture);
		body.setUserData(this);

		Vector3f vec = new Vector3f();

		getOwner().getWorldPosition(vec);
		setPosition(vec.x, vec.y);

		getOwner().getWorldRotation(vec);
		setRotation(vec.z);
		
		setMass(1);
	}

	@Override
	protected void destroyBody() {
		Physics2D physics = getPhysics();
		World world = physics.world;
		world.destroyBody(body);
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

		this.setPosition(posx, posy);
		this.setRotation(posang);
		this.setVelocity(velx, vely);
		this.setRotationVelocity(velang);

	}

	@Override
	public boolean needsReplication(NetworkConnection client) {
		return false;
	}
	
	@Override
	public void render(Camera renderer, float delta) {
	}
	
}
