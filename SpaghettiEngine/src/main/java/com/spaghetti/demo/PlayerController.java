package com.spaghetti.demo;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.spaghetti.audio.SoundBuffer;
import com.spaghetti.audio.SoundSource;
import com.spaghetti.core.GameWindow;
import com.spaghetti.input.Controller;
import com.spaghetti.input.Keys;
import com.spaghetti.interfaces.ToClient;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.utils.CMath;

@ToClient
public class PlayerController extends Controller {

	protected Player player;
	protected RigidBody rb;

	@Override
	protected void onBeginPlay() {
		player = (Player) getOwner();
		rb = player.getComponent(RigidBody.class);
	}

	@Override
	protected void clientUpdate(float delta) {
	}

	@Override
	protected void serverUpdate(float delta) {
	}

	@Override
	protected void commonUpdate(float delta) {
		if (getGame().getActiveController() != this) {
			return;
		}

		Vector2f dir = new Vector2f();
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
			float angle = CMath.lookAt(dir);
			float mod = 10 * getGame().getTickMultiplier(delta);
			float x = (float) Math.cos(angle) * mod;
			float y = (float) Math.sin(angle) * mod;
			Vector3f pos = new Vector3f();
			player.getWorldPosition(pos);
			player.setWorldPosition(pos.x + x, pos.y + y, pos.z);
		}
	}

	@Override
	public void onKeyPressed(int key, int x, int y) {
		switch (key) {
		case Keys.F11:
			GameWindow window = getGame().getWindow();
			window.toggleFullscreen();
			break;
		case Keys.O:
			SoundSource source = new SoundSource(SoundBuffer.get("sb_bruh"));
			source.setDestroyOnStop(true);
			player.addChild(source);
			source.play();
			break;
		}
	}

}
