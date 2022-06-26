package com.spaghetti.demo;

import java.util.Random;

import com.spaghetti.core.Level;
import com.spaghetti.demo.player.Player;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.objects.UntransformedMesh;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody.BodyType;
import com.spaghetti.physics.d2.Physics2D;
import com.spaghetti.physics.d2.RigidBody2D;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

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
		level = getGame().addLevel("myWorld");
		getGame().activateLevel("myWorld");

		// Init physics world
		level.addObject(new Physics2D());

		// Init floor meshes
		for (int i = -100; i <= 100; i++) {
			Mesh floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
			floor.setRelativeScale(2, 2, 1);
			floor.setRelativePosition(i * 2, -3, 0);
			level.addObject(floor);
		}

		// Init floor collision
		floor = new Mesh(Model.get("square"), Material.get("defaultMAT"));
		floor.setRelativeScale(402f, 2, 1);
		floor.setRelativePosition(0, -3, 0);
		floor.setVisible(false);
		floor.addComponent(new RigidBody2D(BodyType.STATIC));
		level.addObject(floor);
		RigidBody2D floor_body = floor.getComponent(RigidBody2D.class);
		floor_body.setFriction(0.3f);

		// Init skybox
		UntransformedMesh skybox = new UntransformedMesh(Model.get("square"), Material.get("m_skybox"));
		skybox.setRelativeScale(50, 50, 1);
		skybox.setRelativeZ(-10);
		level.addObject(skybox);

		// Stress test for physics
		int width = 10;
		int height = 10;
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				Mesh mesh = new Mesh(Model.get("square"), Material.get("defaultMAT"));
				RigidBody2D mesh_body = new RigidBody2D();
				mesh_body.setFriction(0.3f);
				mesh_body.setAngularDamping(0);
				mesh.addComponent(mesh_body);
				mesh.setRelativePosition(i * 1.1f, j * 10, 0);
				mesh.setRelativeScale(2f, 2f, 1);
				level.addObject(mesh);
			}
		}

//		for (int i = 0; i < width; i++) {
//			for(int j = 0; j < height; j++) {
//				Mesh mesh = new Mesh(Model.get("square"), Material.get("defaultMAT"));
//				mesh.addComponent(new RigidBody2D());
//				mesh.setRelativePosition(1000 + i * 10, j * 10, 0);
//				mesh.setRelativeScale(2f, 2f, 1);
//				mesh.setProjectionCaching(false);
//				level.addObject(mesh);
//				mesh.getComponent(RigidBody2D.class).setShape(new Shape<Vector2f>(1));
//				mesh.getComponent(RigidBody2D.class).setMass(1000000);
//				System.out.println("Register rigidbody... " + (1 + ((i * height) + j)) + "/" + (width * height));
//			}
//		}

		// Init local player
		if (!getGame().isMultiplayer()) {
			Player p = new Player();
			p.setRelativePosition(0, 10, 0);
			level.addObject(p);
		}

		// Test physics on moving object
//		MovingMesh mm = new MovingMesh(Model.get("square"), Material.get("defaultMAT"));
//		mm.setRelativeScale(2, 2, 1);
//		RigidBody2D mm_body = new RigidBody2D();
//		mm_body.setFriction(0.3f);
//		mm_body.setAngularDamping(0);
//		mm.addComponent(mm_body);
//		level.addObject(mm);

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
	public void commonUpdate(float delta) {
		i += 10 * getGame().getTickMultiplier(delta);

		float mod = (float) Math.sin(i);
		setRelativePosition((float) Math.cos(random) * mod, (float) Math.sin(random) * mod, 0);
	}

}