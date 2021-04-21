package com.spaghetti.demo;

import org.joml.Vector2d;
import org.joml.Vector3d;

import com.spaghetti.input.Controller;
import com.spaghetti.input.Keys;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;
import com.spaghetti.utils.Logger;

public class PlayerController extends Controller {

	protected Player player;
	protected RigidBody rb;

	@Override
	protected void onBeginPlay() {
		player = (Player) getOwner();
		rb = player.getComponent(RigidBody.class);
//		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
//		String res = "";
//		for(int i = 0; i < stack.length; i++) {
//			res += stack[i].toString() + ((i == stack.length - 1) ? "" : "\n");
//		}
//		System.out.println(res);
		Logger.info("begin play");
	}

	@Override
	protected void onEndPlay() {
		Logger.info("end play");
	}

	@Override
	protected void onDestroy() {
//		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
//		String res = "";
//		for(int i = 0; i < stack.length; i++) {
//			res += stack[i].toString() + ((i == stack.length - 1) ? "" : "\n");
//		}
//		if(Thread.currentThread().getName().equals("SERVER")) System.out.println(res);
		Logger.info("destroy");
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

}
