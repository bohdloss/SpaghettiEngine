package com.spaghetti.input;

import com.spaghetti.core.GameThread;
import com.spaghetti.core.Game;
import com.spaghetti.core.ThreadComponent;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

public class UpdaterComponent implements ThreadComponent {

	protected int fps;
	protected long lastCheck;
	protected Game game;

	@Override
	public void loop(float delta) throws Throwable {
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
	public void initialize(Game game) throws Throwable {
		this.game = game;
	}

	@Override
	public void postInitialize() throws Throwable {
	}

	@Override
	public void preTerminate() throws Throwable {
		game.getGameState().destroy();
	}

	@Override
	public void terminate() throws Throwable {

	}

}
