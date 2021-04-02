package com.spaghetti.demo;

import org.joml.Vector2d;

import com.spaghetti.input.Controller;
import com.spaghetti.input.Keys;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;

public class PlayerController extends Controller {

	protected Player player;
	protected RigidBody rb;

	@Override
	protected void onBeginPlay() {
		player = (Player) getOwner();
		rb = player.getComponent(RigidBody.class);
	}

	@Override
	protected void commonUpdate(double delta) {
		Vector2d dir = new Vector2d();
		if (Keys.keydown(Keys.A)) {
			dir.x -= 1;
		}
		if (Keys.keydown(Keys.D)) {
			dir.x += 1;
		}
		if (Keys.keydown(Keys.W)) {
			dir.y += 1;
		}
		if (Keys.keydown(Keys.S)) {
			dir.y -= 1;
		}

		if (dir.x != 0 || dir.y != 0) {
			double angle = CMath.lookAt(dir);
			double mod = 300;
			double x = Math.cos(angle) * mod;
			double y = Math.sin(angle) * mod;
			rb.applyForce(x, y);
		}
	}

}
