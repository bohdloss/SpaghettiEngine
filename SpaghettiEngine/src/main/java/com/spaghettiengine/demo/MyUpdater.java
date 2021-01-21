package com.spaghettiengine.demo;

import java.awt.Robot;

import com.spaghettiengine.core.*;
import com.spaghettiengine.objects.Camera;
import com.spaghettiengine.objects.Mesh;
import com.spaghettiengine.render.*;

public class MyUpdater extends Updater {

	protected Level level;

	public MyUpdater(Game source) {
		super(source);
	}

	@Override
	public void initialize0() throws Throwable {
		level = new Level();
		source.attachLevel(level);

		Camera camera = new Camera(level, null);
		camera.setFov(3);
		Mesh mesh = new Mesh(level, null, Model.get("apple_model"), Material.get("apple_mat"));
		mesh.addComponent(null);
		
		level.attachCamera(camera);
	}

	double i;

	@Override
	protected void loopEvents(double delta) throws Throwable {
		super.loopEvents(delta);
		i += 0.05 * source.getTickMultiplier(delta);

		level.getObject(Mesh.class).setPitch(i);
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}
