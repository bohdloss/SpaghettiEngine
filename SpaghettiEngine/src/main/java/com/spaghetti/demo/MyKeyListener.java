package com.spaghetti.demo;

import com.spaghetti.core.Game;
import com.spaghetti.input.Keyboard;
import com.spaghetti.input.InputListener;

public class MyKeyListener implements InputListener {

	@Override
	public void onKeyPressed(int key, int x, int y) {
		switch (key) {
		case Keyboard.T:
			Game.getInstance().getClient().disconnect();
			break;
		case Keyboard.P:
			Game.getInstance().getClient().connect("localhost", 9018, 0);
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
	public void onMouseScroll(float xscroll, float yscroll, int x, int y) {

	}

	@Override
	public void onMouseButtonPressed(int button, int x, int y) {

	}

	@Override
	public void onMouseButtonReleased(int button, int x, int y) {

	}

}
