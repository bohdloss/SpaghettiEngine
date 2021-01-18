package com.spaghettiengine.demo;

import java.awt.Robot;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.components.Mesh;
import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;

public class MyUpdater extends Updater {

	protected Level level;
	protected Robot robot;

	public MyUpdater(Game source) {
		super(source);
	}

	@Override
	public void initialize0() throws Throwable {
		robot = new Robot();
		level = new Level();
		source.attachLevel(level);

		Camera camera = new Camera(level, null);
		camera.setFov(3);
		new Mesh(level, null, Model.get("apple_model"), Material.get("apple_mat"));

		level.attachCamera(camera);
	}

	double i;

	@Override
	protected void loopEvents(double delta) throws Throwable {
		super.loopEvents(delta);
		i += 0.05 * source.getTickMultiplier(delta);

		level.getComponent(Mesh.class).setPitch(i);
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}
