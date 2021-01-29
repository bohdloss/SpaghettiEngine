package com.spaghettiengine.interfaces;

public interface Controllable {

	// When a key changes its state
	public void onKeyPressed(int key);
	public void onKeyReleased(int key);
	
	// When the mouse changes its state
	public void onMouseMove(int x, int y);
	public void onMouseScroll(double scroll);
	public void onMouseButtonPressed(int button);
	public void onMouseButtonReleased(int button);
	
	// Continuous input events
	public void ifKeyDown(int key);
	public void ifButtonDown(int button);
	
}