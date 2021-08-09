package com.spaghetti.demo.player;

import com.spaghetti.core.GameObject;
import com.spaghetti.input.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import com.spaghetti.utils.Logger;

@Bidirectional
public class Player extends GameObject {

	public static Controller<Player> createController() {
		PlayerController<Player> controller = new PlayerController<Player>();
		controller.registerCommands("jump", self -> self.jump(), self -> self.jump_stop());
		controller.registerCommands("right", self -> self.right(), self -> self.right_stop());
		controller.registerCommands("left", self -> self.left(), self -> self.left_stop());
		
		controller.registerCommand("scrollnotify", (ParameterizedControllerAction<Player>) (self, args) -> {
			self.scrollnotify((float) args[0], (float) args[1]);
		});
		
		controller.bindKeyCmd(Keyboard.A, "left", "!left");
		controller.bindKeyCmd(Keyboard.D, "right", "!right");
		controller.bindKeyCmd(Keyboard.SPACE, "jump", "!jump");
		controller.bindScrollCmd("scrollnotify");
		return controller;
	}
	
	protected Camera camera;
	protected Controller<Player> controller;
	protected RigidBody2D body;
	protected Mesh mesh;
	
	protected boolean left, right;
	
	public Player() {
	}
	
	@Override
	protected void onBeginPlay() {
		if(getGame().hasAuthority()) {
			// Initialize camera
			camera = new Camera();
			camera.setFov(30);
			addChild(camera);
			getGame().attachCamera(camera);
			
			// Initialize controller
			addComponent(controller = createController());
			
			// Initialize body
			body = new RigidBody2D();
			addComponent(body);
			body.setCanRotate(false);
			body.setRotation(1f);
			
			// Initialize player model
			mesh = new Mesh();
			mesh.setMaterial(Material.get("defaultMAT"));
			mesh.setModel(Model.get("square"));
			mesh.setProjectionCaching(false);
			addChild(mesh);
			
			// Adjust scale
			setRelativeScale(1, 2, 1);
		}
	}
	
	@Override
	protected void onEndPlay() {
		if(getGame().hasAuthority()) {
			getGame().detachCamera();
			camera = null;
		}
	}

	@Override
	protected void commonUpdate(float delta) {
		float force = 20 * ((left ? -1 : 0) + (right ? 1 : 0));
		body.applyForce(force, 0);
	}
	
	// Player actions
	
	public void jump() {
		body.applyVelocity(0, 5);
	}
	
	public void jump_stop() {
		Logger.info("Stop jumping...");
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
	
	public void scrollnotify(float x, float y) {
		Logger.info("Scrolled x: " + x + " y: " + y);
	}
	
	// Getters and setters
	
	public Camera getCamera() {
		return camera;
	}
	
}
