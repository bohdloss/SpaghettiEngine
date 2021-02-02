package com.spaghettiengine.core;

import org.lwjgl.glfw.GLFW;

import com.spaghettiengine.utils.FunctionDispatcher;
import com.spaghettiengine.utils.Utils;

public final class Handler extends Thread {

	protected boolean stop;
	protected boolean stopOnNoActivity;
	protected FunctionDispatcher dispatcher;

	public Handler() {
		super("HANDLER");
		GLFW.glfwInit();
		dispatcher = new FunctionDispatcher();
		dispatcher.setDefaultId(getId());
	}

	@Override
	public void run() {
		while (!stop) {
			Utils.sleep(1);

			// This makes sure windows can be interacted with
			// In Windows this also unties the renderer from
			// any window event
			GameWindow.pollEvents();
			dispatcher.computeEvents();

			try {

				boolean found = false;
				for (Game game : Game.games) {
					// Detect soft-blocked instances and stop()
					if (!game.isStopped()) {
						if(game.isInit() && (game.isDead() || game.stopSignal)) {
							game.stop();
						}
					}

					// Detect if all games are stopped
					// and if the stopOnNoActivity flag
					// is on then end this thread;
					if (!game.isStopped()) {
						found = true;
					}
				}
				;

				if (!found && stopOnNoActivity) {
					stop = true;
				}

			} catch (Exception e) {
			}

		}
	}

}
