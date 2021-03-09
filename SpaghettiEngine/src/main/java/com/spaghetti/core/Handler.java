package com.spaghetti.core;

import org.lwjgl.glfw.GLFW;

import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.Utils;

public final class Handler extends Thread {

	protected boolean stop;
	protected boolean stopOnNoActivity;
	protected FunctionDispatcher dispatcher;

	public Handler() {
		super("HANDLER");
	}

	@Override
	public void run() {
		GLFW.glfwInit();
		dispatcher = new FunctionDispatcher();
		while (!stop) {
			try {
				Utils.sleep(1);

				// This makes sure windows can be interacted with
				// In Windows this also unties the renderer from
				// any window event
				GameWindow.pollEvents();
				dispatcher.computeEvents();

				boolean found = false;
				for (Game game : Game.games) {
					for (Game dependency : game.dependencies) {
						if (dependency.isStopped() || dependency.isDead()) {
							game.stopSignal = true;
						}
					}
					// Detect soft-blocked instances and stop()
					if (!game.isStopped()) {
						if (game.isInit() && (game.isDead() || game.stopSignal)) {
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
			} catch (Throwable t) {
				// Catch anything because we can't have this thread die
			}
		}
	}

}
