package com.spaghetti.demo;

import com.spaghetti.interfaces.JoinHandler;
import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.objects.Camera;

public class MyJoinHandler implements JoinHandler {

	@Override
	public void handleJoin(boolean isClient, NetworkConnection worker) {
		if (!isClient) {
			worker.player = new Player();
			worker.getLevel().addObject(worker.player);
			worker.player_camera = worker.player.getChild(Camera.class);
			worker.player_controller = worker.player.getComponent(PlayerController.class);
		}
	}

}
