package com.spaghetti.core;

import com.spaghetti.input.Controller;
import com.spaghetti.objects.Camera;

public class ClientState {

	protected final Game game;

	public ClientState(Game game) {
		this.game = game;
	}

	protected float tickMultiplier = 1;

	protected Level activeLevel;
	protected Camera activeCamera;
	protected Controller activeController;

	// Getters and setters

	public Level getActiveLevel() {
		return activeLevel;
	}

	public void detachLevel() {
		if (activeLevel == null) {
			return;
		}
		activeLevel.source = null;
		activeLevel = null;
		detachCamera();
		detachController();
	}

	public void attachLevel(Level level) {
		if (level == null) {
			return;
		}
		if (activeLevel != null) {
			detachLevel();
		}
		activeLevel = level;
		activeLevel.source = game;
	}

	public Camera getActiveCamera() {
		return activeCamera;
	}

	public void detachCamera() {
		if (activeCamera == null) {
			return;
		}
		activeCamera = null;
	}

	public void attachCamera(Camera camera) {
		if (camera == null || game.isHeadless() || activeCamera == camera) {
			return;
		}
		if (activeCamera != null) {
			detachCamera();
		}
		camera.calcScale();
		activeCamera = camera;
	}

	public Controller getActiveController() {
		return activeController;
	}

	public void detachController() {
		if (activeController == null) {
			return;
		}
		activeController = null;
	}

	public void attachController(Controller controller) {
		if (controller == null || game.isHeadless() || activeController == controller) {
			return;
		}
		if (activeController != null) {
			detachController();
		}
		this.activeController = controller;
	}

	public float getTickMultiplier() {
		return tickMultiplier;
	}

	public void setTickMultiplier(float multiplier) {
		this.tickMultiplier = multiplier;
	}

}
