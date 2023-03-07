package com.spaghetti.demo;

import com.spaghetti.core.Game;
import com.spaghetti.input.InputListener;
import com.spaghetti.input.Keyboard;

public class FullscreenListener implements InputListener {

	@Override
	public void onKeyPressed(int key, int x, int y) {
		switch (key) {
			case Keyboard.F11:
			Game.getInstance().getWindow().toggleFullscreen();
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
