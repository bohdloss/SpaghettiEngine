package com.spaghetti.input;

public class MouseInput extends InputDevice implements InputListener {

	@Override
	public void onBeginPlay() {
		getGame().getInputDispatcher().registerListener(this);
	}

	@Override
	public void onEndPlay() {
		getGame().getInputDispatcher().unregisterListener(this);
	}

	@Override
	public void onMouseMove(int x, int y) {

	}

	@Override
	public void onMouseScroll(float xscroll, float yscroll, int x, int y) {

	}

	@Override
	public void onMouseButtonPressed(int button, int x, int y) {

	}

	@Override
	public void onMouseButtonReleased(int button, int x, int y) {

	}

	@Override
	public void onKeyPressed(int key, int x, int y) {}
	@Override
	public void onKeyReleased(int key, int x, int y) {}

}
