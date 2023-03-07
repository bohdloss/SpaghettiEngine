package com.spaghetti.core;

import com.spaghetti.input.Controller;
import com.spaghetti.render.Camera;
import com.spaghetti.world.GameObject;

/**
 * ClientState can be subject to changes by the server, but for the most part is
 * different on each client, or should be considered as such
 * <p>
 * The class holds data that is necessary for the client and doesn't often need
 * server synchronization
 *
 * @author bohdloss
 *
 */
public class ClientState {

	protected final Game game;

	protected GameObject player;
	protected Camera camera;
	protected Controller<?> controller;

	public ClientState(Game game) {
		this.game = game;
	}
	
	// Getters and setters

	public void setLocalCamera(Camera camera) {
		if (this.camera == camera) {
			return;
		}
		if (camera != null) {
			camera.calcScale();
		}
		this.camera = camera;
	}

	public Camera getLocalCamera() {
		return camera;
	}

	public void setLocalPlayer(GameObject player) {
		this.player = player;
	}

	public GameObject getLocalPlayer() {
		return player;
	}

	public void setLocalController(Controller<?> controller) {
		this.controller = controller;
	}

	public Controller<?> getLocalController() {
		return controller;
	}

}
