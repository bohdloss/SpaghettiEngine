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

	@Override
	public void initialize0() throws Throwable {
		if (!getGame().hasAuthority()) {
//			return;
		}
		
		level = new Level();
		getGame().attachLevel(level);
		Camera camera = new Camera(level, null);
		camera.setFov(20);

		floor = new Mesh(level, null, Model.get("square"), Material.get("defaultMAT"));
		floor.setRelativeScale(15, 2, 1);
		floor.setRelativePosition(0, -3, 0);

		square = new MovingMesh(level, null, Model.get("apple_model"), Material.get("apple_mat"));
//		for(int i = 0; i < 200; i++) {
//			new MovingMesh(level, null, Model.get("apple_model"), Material.get("apple_mat"));
//		}
		
		
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

	public MovingMesh(Level level, GameObject parent, Model model, Material material) {
		super(level, parent, model, material);
	}
	
	public MovingMesh(Level level, GameObject parent) {
		super(level, parent);
	}
	
	double random = new Random().nextDouble() * 7;
	double i = 0;

	@Override
	public void commonUpdate(double delta) {
		i += 10 * getGame().getTickMultiplier(delta);

		double mod = Math.sin(i);
		
		setRelativePosition(Math.cos(random) * mod, Math.sin(random) * mod, 0);
	}
	
}