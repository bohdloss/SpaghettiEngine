package com.spaghetti.demo;

import java.util.Random;

import com.spaghetti.core.*;
import com.spaghetti.demo.player.Player;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.objects.UntransformedMesh;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody.BodyType;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.*;

public class MyUpdater extends UpdaterCore {

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

		for (int i = -100; i <= 100; i++) {
			Mesh floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
			floor.setRelativeScale(2, 2, 1);
			floor.setRelativePosition(i * 2, -3, -5);
			level.addObject(floor);
		}
		floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
		floor.setRelativeScale(401, 2, 1);
		floor.setRelativePosition(0, -3, 0);
		floor.setVisible(false);
		floor.addComponent(new RigidBody2D(BodyType.STATIC));
		level.addObject(floor);
		RigidBody2D floor_body = floor.getComponent(RigidBody2D.class);
		floor_body.setFriction(0.3f);

		if (!getGame().isMultiplayer()) {
			level.addObject(new Player());
		}

		UntransformedMesh skybox = new UntransformedMesh(Model.get("square"), Material.get("m_skybox"));
		skybox.setRelativeScale(20, 20, 1);
		skybox.setRelativeZ(-10);
		level.addObject(skybox);

//		for (int i = 0; i < 100; i++) {
//			Mesh mesh = new Mesh(Model.get("square"), Material.get("defaultMAT"));
//			mesh.addComponent(new RigidBody2D());
//			level.addObject(mesh);
//			RigidBody2D rb = mesh.getComponent(RigidBody2D.class);
//		}
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