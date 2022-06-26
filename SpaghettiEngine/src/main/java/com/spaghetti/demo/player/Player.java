package com.spaghetti.demo.player;

import com.spaghetti.core.GameObject;
import com.spaghetti.input.Keyboard;
import com.spaghetti.input.KeyboardInput;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import com.spaghetti.utils.CMath;

public class Player extends GameObject {

	public static KeyboardInput createKeyBindings() {
		KeyboardInput input = new KeyboardInput();

		input.bindKeyCmd(Keyboard.A, "+left", "-left");
		input.bindKeyCmd(Keyboard.D, "+right", "-right");
		input.bindKeyCmd(Keyboard.W, "+up", "-up");
		input.bindKeyCmd(Keyboard.S, "+down", "-down");
		input.bindKeyCmd(Keyboard.SPACE, "+jump", "-jump");
		input.bindScrollCmd("scrollnotify");
		return input;
	}

	protected Camera camera;
	protected PlayerController controller;
	protected KeyboardInput input;
	protected RigidBody2D body;
	protected Mesh mesh;

	protected boolean left, right, up, down;

	public Player() {
		if (getGame().hasAuthority()) {
			// Initialize camera
			camera = new Camera();
			camera.setFov(50);
			addChild(camera);
			getGame().setLocalCamera(camera);

			// Initialize controller
			addComponent(controller = new PlayerController());
			addComponent(input = createKeyBindings());
			input.bindController(controller);

			// Initialize body
			body = new RigidBody2D();
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
	}

	@Override
	protected void onEndPlay() {
		if (getGame().hasAuthority()) {
			getGame().setLocalCamera(null);
			camera = null;
		}
	}

	@Override
	protected void commonUpdate(float delta) {
		float multx = (left ? -1 : 0) + (right ? 1 : 0);
		float multy = (down ? -1 : 0) + (up ? 1 : 0);
		float angle = CMath.lookAt(multx, multy);
		float forcex = (float) Math.cos(angle) * 1;
		float forcey = (float) Math.sin(angle) * 1;

		if (multx != 0 || multy != 0) {
			body.applyForce(forcex, forcey);
		}
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

	public void scrollnotify(float x, float y) {
		camera.setFov(camera.getFov() + y * 10);
	}

	// Getters and setters

	public Camera getCamera() {
		return camera;
	}

}
