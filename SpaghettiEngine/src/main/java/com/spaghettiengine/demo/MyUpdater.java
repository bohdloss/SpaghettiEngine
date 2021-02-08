package com.spaghettiengine.demo;

import org.joml.Vector3d;

import com.spaghettiengine.core.*;
import com.spaghettiengine.input.Controller;
import com.spaghettiengine.input.Keys;
import com.spaghettiengine.input.Updater;
import com.spaghettiengine.objects.Camera;
import com.spaghettiengine.objects.Mesh;
import com.spaghettiengine.physics.Physics;
import com.spaghettiengine.physics.RigidBody;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.CMath;

public class MyUpdater extends Updater {

	protected Level level;
	protected Physics physics;
	protected Mesh square;
	protected Mesh square2;
	protected Mesh floor;

	@Override
	public void initialize0() throws Throwable {
		level = new Level();
		getSource().attachLevel(level);
		Camera camera = new Camera(level, null);
		camera.setFov(20);

		physics = new Physics(level);
		physics.setGravity(0, 0);
		square = new Mesh(level, null, Model.get("apple_model"), Material.get("apple_mat")) {
			public void commonUpdate(double delta) {
				super.commonUpdate(delta);
				Vector3d vec = new Vector3d();
				getWorldPosition(vec);
				getLevel().getObject(Camera.class).setWorldPosition(vec);
			}
		};
		square2 = new Mesh(level, null, Model.get("apple_model"), Material.get("defaultMAT"));
		floor = new Mesh(level, null, Model.get("square"), Material.get("defaultMAT"));

		square2.setRelativeScale(1, 1, 1);
		square2.setRelativePosition(1, 2, 0);

		square.setRelativeScale(1, 1, 1);
		square.setRelativePosition(-1, 2, 0);
		square.addComponent(new RigidBody());
		square.addComponent(new Controller() {

			double _speed = 1;
			
			@Override
			public void commonUpdate(double delta) {
				
				double x = 0;
				double y = 0;
				
				if(Keys.keydown(Keys.D)) {
					x += 1;
				}
				if(Keys.keydown(Keys.A)) {
					x -= 1;
				}
				if(Keys.keydown(Keys.W)) {
					y += 1;
				}
				if(Keys.keydown(Keys.S)) {
					y -= 1;
				}
				
				if(x == 0 && y == 0) {
					return;
				}
				
				double angle = CMath.lookAt(x, y);
				
				double force_x = Math.cos(angle) * 200 * _speed;
				double force_y = Math.sin(angle) * 200 * _speed;
				
				getOwner().getComponent(RigidBody.class).applyForce(force_x, force_y);
				
			}
			
			@Override
			public void onMouseScroll(double scroll, int x, int y) {
				_speed += scroll;
				_speed = _speed < 0 ? 0 : _speed;
			}

			@Override
			public void onKeyPressed(int key, int x, int y) {
				if(key == Keys.R) {
					getOwner().setWorldPosition(0, 0, 0);
				}
			}
			
		});

//		square.addChild(square2);
		square2.setWorldX(0);

		floor.setRelativeScale(15, 2, 1);
		floor.setRelativePosition(0, -3, 0);

		level.attachCamera(camera);
	}

	double i;
	boolean slowmode;
	long last;

	@Override
	protected void loopEvents(double delta) throws Throwable {
		super.loopEvents(delta);
//		System.out.println(level.getObjectAmount());
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}
