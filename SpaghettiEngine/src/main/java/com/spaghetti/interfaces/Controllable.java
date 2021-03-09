package com.spaghetti.interfaces;

public interface Controllable {

	// When a key changes its state
	public void onKeyPressed(int key, int x, int y);

	public void onKeyReleased(int key, int x, int y);

	// When the mouse changes its state
	public void onMouseMove(int x, int y);

	public void onMouseScroll(double scroll, int x, int y);

	public void onMouseButtonPressed(int button, int x, int y);

	public void onMouseButtonReleased(int button, int x, int y);

}