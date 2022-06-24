package com.spaghetti.input;

import java.util.ArrayList;

import com.spaghetti.core.GameComponent;

public class InputDevice extends GameComponent {

	protected ArrayList<Controller<?>> controllers = new ArrayList<>();

	public void bindController(Controller<?> controller) {
		if (controller != null && !controllers.contains(controller)) {
			controllers.add(controller);
		}
	}

	public void unbindController(Controller<?> controller) {
		controllers.remove(controller);
	}

}
