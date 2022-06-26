package com.spaghetti.input;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.Game;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public class UpdaterCore extends CoreComponent {

	@Override
	protected void loopEvents(float delta) throws Throwable {
		Game game = getGame();
		try {
			if (!game.isHeadless()) {
				game.getInputDispatcher().update();
			}
		} catch (Throwable t) {
			Logger.error("Updater error:", t);
		}

		try {
			game.getGameState().update(delta);
		} catch (Throwable t) {
			Logger.error("Level generated an exception:", t);
			Utils.sleep(100); // Remove later
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void preTerminate() throws Throwable {
		getGame().getGameState().destroyAllLevels();
	}

	@Override
	protected void terminate0() throws Throwable {

	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getUpdater();
	}

}
