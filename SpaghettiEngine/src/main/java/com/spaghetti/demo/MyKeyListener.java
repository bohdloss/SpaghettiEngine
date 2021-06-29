package com.spaghetti.demo;

import com.spaghetti.core.Game;
import com.spaghetti.input.Keys;
import com.spaghetti.interfaces.Controllable;

public class MyKeyListener implements Controllable {

	@Override
	public void onKeyPressed(int key, int x, int y) {
		switch(key) {
		case Keys.T:
			Game.getGame().getClient().disconnect();
			break;
		case Keys.O:
			Game.getGame().getClient().connect("localhost", 9018);
			break;
		}
	}

	@Override
	public void onKeyReleased(int key, int x, int y) {
		
	}

	@Override
	public void onMouseMove(int x, int y) {
		
	}

	@Override
	public void onMouseScroll(float scroll, int x, int y) {
		
	}

	@Override
	public void onMouseButtonPressed(int button, int x, int y) {
		
	}

	@Override
	public void onMouseButtonReleased(int button, int x, int y) {
		
	}

}
