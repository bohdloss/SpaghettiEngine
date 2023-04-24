package com.spaghetti.core;

import com.spaghetti.utils.ThreadUtil;

public final class HandlerThread extends Thread {

	protected boolean stop;
	protected boolean started;

	public HandlerThread() {
		super("HANDLER");
		setPriority(MIN_PRIORITY);
	}

	@Override
	public void run() {
		while (!stop) {
			try {
				ThreadUtil.sleep(500);

				boolean found = false;
				for (Game game : Game.games) {
					started = true;
					for (Game dependency : game.dependencies) {
						if (dependency.isStopped() || dependency.isDead()) {
							game.stopAsync();
						}
					}

					// Detect soft-blocked instances and stop()
					if (!game.isStopped()) {
						if (game.isInit() && (game.isDead() || game.stopSignal)) {
							game.stop();
						}
					}

					// Detect if all games are stopped
					// and then end this thread
					if (!game.isStopped()) {
						found = true;
					}
				}

				if (!found && started) {
					stop = true;
				}
			} catch (Throwable t) {
				// Catch anything because we can't have this thread die
			}
		}
		Game.handlerThread = null;
	}

}
