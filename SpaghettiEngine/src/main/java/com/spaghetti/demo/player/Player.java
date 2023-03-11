package com.spaghetti.demo.player;

import com.spaghetti.demo.DemoMode;
import com.spaghetti.physics.d2.jbox2d.JBox2DRigidBody;
import com.spaghetti.render.camera.PerspectiveCamera;
import com.spaghetti.world.GameObject;
import com.spaghetti.input.Keyboard;
import com.spaghetti.input.KeyboardInput;
import com.spaghetti.render.Camera;
import com.spaghetti.render.Mesh;
import com.spaghetti.physics.d2.Physics2D;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import com.spaghetti.utils.MathUtil;
import org.joml.Vector2f;

public class Player extends GameObject {

	public static KeyboardInput createKeyBindings() {
		KeyboardInput input = new KeyboardInput();

		input.bindKeyCmd(Keyboard.A, "+left", "-left");
		input.bindKeyCmd(Keyboard.D, "+right", "-right");
		input.bindKeyCmd(Keyboard.W, "+up", "-up");
		input.bindKeyCmd(Keyboard.S, "+down", "-down");
		input.bindKeyCmd(Keyboard.SPACE, "+jump", "-jump");
		input.bindKeyCmd(Keyboard.F, "+explode", "-explode");
		input.bindKeyCmd(Keyboard.Q, "+lefttilt", "-lefttilt");
		input.bindKeyCmd(Keyboard.E, "+righttilt", "-righttilt");
		input.bindScrollCmd("scrollnotify");
		return input;
	}

	protected Camera camera;
	protected PlayerController controller;
	protected KeyboardInput input;
	protected RigidBody2D body;
	protected Mesh mesh;

	protected boolean left, right, up, down;

	protected boolean leftTilt, rightTilt;

	public Player() {
		if (getGame().hasAuthority()) {
			// Initialize camera
			camera = new Camera();
			camera.setFov(50);
			addChild(camera);

			// Initialize controller
			addComponent(controller = new PlayerController());
			addComponent(input = createKeyBindings());
			input.bindController(controller);

			// Initialize body
			body = RigidBody2D.getInstance();
			addComponent(body);
			body.setCanRotate(false);
			body.setFriction(0.3f);

			// Initialize player model
			mesh = new Mesh();
			mesh.setMaterial(Material.get("defaultMAT"));
			mesh.setModel(Model.get("square"));
			addChild(mesh);

			// Adjust scale
			setRelativeScale(1, 2, 1);
		}
	}

	@Override
	protected void onBeginPlay() {
		body = getComponent(RigidBody2D.class);
		mesh = getChild(Mesh.class);
		controller = getComponent(PlayerController.class);
		camera = getChild(Camera.class);
		getGame().setLocalCamera(camera);
	}

	@Override
	protected void onEndPlay() {
		camera = null;
	}

	@Override
	protected void commonUpdate(float delta) {
		float multx = (left ? -1 : 0) + (right ? 1 : 0);
		float multy = (down ? -1 : 0) + (up ? 1 : 0);
		float angle = MathUtil.lookAt(multx, multy);
		float forcex = (float) Math.cos(angle) * 10000;
		float forcey = (float) Math.sin(angle) * 10000;

		if (multx != 0 || multy != 0) {
			body.applyForce(forcex * delta, forcey * delta);
		}

		float tiltDir = 0;
		if(leftTilt) {
			tiltDir += 1;
		}
		if(rightTilt) {
			tiltDir -= 1;
		}

		GameObject container = ((DemoMode) getGame().getGameState().getGameMode()).getFloorContainer();
		container.setRoll(container.getRoll() + 0.1f * tiltDir * delta);
	}

	// Player actions

	public void jump() {
		body.applyVelocity(0, 5);
	}

	public void jump_stop() {
	}

	public void right() {
		right = true;
	}

	public void right_stop() {
		right = false;
	}

	public void left() {
		left = true;
	}

	public void left_stop() {
		left = false;
	}

	public void up() {
		up = true;
	}

	public void up_stop() {
		up = false;
	}

	public void down() {
		down = true;
	}

	public void down_stop() {
		down = false;
	}

	public void explode() {
		Physics2D physics = getLevel().getObject(Physics2D.class);
		Vector2f our = new Vector2f();
		body.getPosition(our);
		for(int i = 0; i < physics.getBodyCount(); i++) {
			RigidBody2D body = physics.getBodyAt(i);
			if(body != this.body) {
				Vector2f other = new Vector2f();
				body.getPosition(other);

				float angle = MathUtil.lookAt(our, other);
				float force = 100000 / (float) (Math.pow(MathUtil.distance(our.x, our.y, other.x, other.y), 2));
				float forcex = (float) (force * Math.cos(angle));
				float forcey = (float) (force * Math.sin(angle));
				body.applyForce(forcex, forcey);
			}
		}
	}

	public void explode_stop() {}

	public void leftTilt() {
		leftTilt = true;
	}

	public void leftTilt_stop() {
		leftTilt = false;
	}

	public void rightTilt() {
		rightTilt = true;
	}

	public void rightTilt_stop() {
		rightTilt = false;
	}

	public void scrollnotify(float x, float y) {
		camera.setFov(camera.getFov() + y * 10);
	}

	// Getters and setters

	public Camera getCamera() {
		return camera;
	}

}
