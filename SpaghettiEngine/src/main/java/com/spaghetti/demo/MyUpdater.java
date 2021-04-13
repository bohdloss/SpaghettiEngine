package com.spaghetti.demo;

import java.util.Random;

import com.spaghetti.core.*;
import com.spaghetti.input.Updater;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.physics.Physics;
import com.spaghetti.render.*;

public class MyUpdater extends Updater {

	protected Level level;
	protected Physics physics;
	protected Mesh square;
	protected Mesh square2;
	protected Mesh floor;
	protected Camera camera;
	protected Player player;

	@Override
	public void initialize0() throws Throwable {
		if (!getGame().hasAuthority()) {
			return;
		}
		level = new Level();
		getGame().attachLevel(level);

		camera = new Camera(level);
		camera.setFov(20);

		floor = new Mesh(level, Model.get("square"), Material.get("defaultMAT"));
		floor.setRelativeScale(15, 2, 1);
		floor.setRelativePosition(0, -3, 0);

		if(!getGame().isMultiplayer()) {
			level.addObject(new Player(level));
		}

		level.addObject(camera);
		level.addObject(floor);
		level.attachCamera(camera);
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		super.loopEvents(delta);
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}

class MovingMesh extends Mesh {

	public MovingMesh(Level level, Model model, Material material) {
		super(level, model, material);
	}

	public MovingMesh(Level level) {
		super(level);
	}

	double random = new Random().nextDouble() * 7;

	double i = 0;

	@Override
	public void serverUpdate(double delta) {
		i += 10 * getGame().getTickMultiplier(delta);

		double mod = Math.sin(i);
		setRelativePosition(Math.cos(random) * mod, Math.sin(random) * mod, 0);
	}

}