package com.spaghetti.input;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.Game;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

public class UpdaterCore extends CoreComponent {

	protected int fps;
	protected long lastCheck;

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
			// Prevent exception spam (exceptions too fast may prevent the stacktrace from generating)
			ThreadUtil.sleep(100);
		}
		fps++;
		if (System.currentTimeMillis() >= lastCheck + 1000) {
			Logger.info(fps + " UPS");
			fps = 0;
			lastCheck = System.currentTimeMillis();
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void preTerminate() throws Throwable {
		getGame().getGameState().destroy();
	}

	@Override
	protected void terminate0() throws Throwable {

	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getUpdater();
	}

}
