package com.spaghetti.input;

import com.spaghetti.core.CoreComponent;

public class Updater extends CoreComponent {

	@Override
	protected void loopEvents(double delta) throws Throwable {

		if (!getGame().isHeadless()) {
			getGame().getAssetManager().lazyLoad();
			getGame().getWindow().getInputDispatcher().update();
		}

		if (getLevel() != null) {
			getLevel().update(delta);
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void terminate0() throws Throwable {
		if (getGame().getActiveLevel() != null) {
			getGame().getActiveLevel().destroy();
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getUpdater();
	}

}
