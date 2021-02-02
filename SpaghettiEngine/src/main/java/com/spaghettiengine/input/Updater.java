package com.spaghettiengine.input;

import com.spaghettiengine.core.CoreComponent;
import com.spaghettiengine.core.Game;

public class Updater extends CoreComponent {

	@Override
	protected void loopEvents(double delta) throws Throwable {
		getSource().getFunctionDispatcher().computeEvents();
		
		getSource().getWindow().getInputDispatcher().update();
		
		if (getSource().getActiveLevel() != null) {
			getSource().getActiveLevel().update(delta);
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void terminate0() throws Throwable {
		if (getSource().getActiveLevel() != null) {
			getSource().getActiveLevel().destroy();
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getSource().getUpdater();
	}
	
}
