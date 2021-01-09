package com.spaghettiengine.demo;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.components.Mesh;
import com.spaghettiengine.core.*;
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

		Camera camera = new Camera(level, null, source.getWindow().getWidth(), source.getWindow().getHeight());
		camera.setFov(3);
		new Mesh(level, null, Model.get("apple_model"), Material.get("apple_mat"));
		
		level.attachCamera(camera);
	}

	double i;

	@Override
	protected void loopEvents() throws Throwable {
		super.loopEvents();
		i += 0.001;
		
		level.getComponent(1).setRotation(i);
	}

}
