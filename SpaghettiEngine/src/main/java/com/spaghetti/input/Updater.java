package com.spaghetti.input;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.utils.Logger;

public class Updater extends CoreComponent {

	@Override
	protected void loopEvents(float delta) throws Throwable {
		try {
			if (!getGame().isHeadless()) {
				getGame().getAssetManager().lazyLoad();
				getGame().getInputDispatcher().update();
			}

			if (getLevel() != null) {
				getLevel().update(delta);
			}
		} catch (Throwable t) {
			Logger.error("Level generated an exception: ", t);
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void preTerminate() throws Throwable {
		if (getGame().getActiveLevel() != null) {
			getGame().getActiveLevel().destroy();
		}
	}

	@Override
	protected void terminate0() throws Throwable {

	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getUpdater();
	}

}
