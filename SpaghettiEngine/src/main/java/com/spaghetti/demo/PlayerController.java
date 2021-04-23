package com.spaghetti.demo;

import org.joml.Vector2d;
import org.joml.Vector3d;

import com.spaghetti.core.GameWindow;
import com.spaghetti.input.Controller;
import com.spaghetti.input.Keys;
import com.spaghetti.interfaces.*;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;

@ToServer
public class PlayerController extends Controller {

	protected Player player;
	protected RigidBody rb;

	@Override
	protected void onBeginPlay() {
		player = (Player) getOwner();
		rb = player.getComponent(RigidBody.class);
	}

	@Override
	protected void clientUpdate(double delta) {
	}

	@Override
	protected void serverUpdate(double delta) {
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
			double mod = 10 * getGame().getTickMultiplier(delta);
			double x = Math.cos(angle) * mod;
			double y = Math.sin(angle) * mod;
			Vector3d pos = new Vector3d();
			player.getWorldPosition(pos);
			player.setWorldPosition(pos.x + x, pos.y + y, pos.z);
		}
	}

	@Override
	public void onKeyPressed(int key, int x, int y) {
		if (key == Keys.F11) {
			GameWindow window = getGame().getWindow();
			window.toggleFullscreen();
		}
	}

}
