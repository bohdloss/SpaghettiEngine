package com.spaghetti.demo;

import java.util.Random;

import com.spaghetti.core.*;
import com.spaghetti.input.Updater;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.objects.SkyboxMesh;
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

		floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
		floor.setRelativeScale(15, 2, 1);
		floor.setRelativePosition(0, -3, -5);

		if (!getGame().isMultiplayer()) {
			level.addObject(new Player());
		}

		SkyboxMesh skybox = new SkyboxMesh(Model.get("square"), Material.get("m_skybox"));
		skybox.setRelativeScale(20, 20, 1);
		skybox.setRelativeZ(-10);
		level.addObject(skybox);
		level.addObject(floor);

		for (int i = 0; i < 100; i++) {
//			level.addObject(new Mesh(Model.get("square"), Material.get("defaultMAT")));
		}
	}

	boolean done = false;

	@Override
	protected void loopEvents(float delta) throws Throwable {
		super.loopEvents(delta);
	}

	@Override
	protected void terminate0() throws Throwable {
		super.terminate0();
	}

}

class MovingMesh extends Mesh {

	public MovingMesh(Model model, Material material) {
		super(model, material);
	}

	public MovingMesh() {
		super();
	}

	float random = new Random().nextFloat() * 7;

	float i = 0;

	@Override
	public void serverUpdate(float delta) {
		i += 10 * getGame().getTickMultiplier(delta);

		float mod = (float) Math.sin(i);
		setRelativePosition((float) Math.cos(random) * mod, (float) Math.sin(random) * mod, 0);
	}

}