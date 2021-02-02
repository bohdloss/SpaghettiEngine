package com.spaghettiengine.demo;

import com.spaghettiengine.components.Controller;
import com.spaghettiengine.core.*;
import com.spaghettiengine.input.Keys;
import com.spaghettiengine.input.Updater;
import com.spaghettiengine.objects.Camera;
import com.spaghettiengine.objects.Mesh;
import com.spaghettiengine.physics.RigidBody;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.Utils;

public class MyUpdater extends Updater {

	protected Level level;

	@Override
	public void initialize0() throws Throwable {
		level = new Level();
		getSource().attachLevel(level);
		Camera camera = new Camera(level, null);
		camera.setFov(20);
		
		Mesh square = new Mesh(level, null, Model.get("square"), Material.get("defaultMAT"));
		square.setRelativeScale(1, 1, 1);
		square.setRelativePosition(-1, 2, 0);
		square.addComponent(new Controller() {
			double speed = 0.0001;
			public void ifKeyDown(int key, int x, int y) {
				if(key == Keys.D) {
					getOwner().setRelativeX(getOwner().getRelativeX() + speed);
				}
				if(key == Keys.A) {
					getOwner().setRelativeX(getOwner().getRelativeX() - speed);
				}
				if(key == Keys.W) {
					getOwner().setRelativeY(getOwner().getRelativeY() + speed);
				}
				if(key == Keys.S) {
					getOwner().setRelativeY(getOwner().getRelativeY() - speed);
				}
			}
			
			public void onMouseScroll(double scroll, int x, int y) {
				speed += scroll / 10000;
				speed = speed < 0 ? 0 : speed;
			}
			
		});
		
		Mesh square2 = new Mesh(level, null, Model.get("square"), Material.get("defaultMAT"));
		square2.setRelativeScale(1, 1, 1);
		square2.setRelativePosition(1, 2, 0);
		
		Mesh floor = new Mesh(level, null, Model.get("square"), Material.get("defaultMAT"));
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
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}
