package com.spaghettiengine.components;

import com.spaghettiengine.core.*;
import com.spaghettiengine.interfaces.*;

// Why replicate a controller in multiplayer???
// You should control only your character!
@NoReplicate
public class Controller extends GameComponent implements Controllable {

	@Override
	public void onKeyPressed(int key) {}

	@Override
	public void onKeyReleased(int key) {}

	@Override
	public void onMouseMove(int x, int y) {}

	@Override
	public void onMouseScroll(double scroll) {}

	@Override
	public void onMouseButtonPressed(int button) {}

	@Override
	public void onMouseButtonReleased(int button) {}

	@Override
	public void ifKeyDown(int key) {}

	@Override
	public void ifButtonDown(int button) {}
	
	@Override
	public void onBeginPlay() {
		GameWindow window = getGame().getWindow();
		if(window != null) {
			window.getInputDispatcher().registerListener(this);
		}
	}
	
	@Override
	public void onEndPlay() {
		GameWindow window = getGame().getWindow();
		if(window != null) {
			window.getInputDispatcher().unregisterListener(this);
		}
	}

	
	
}
