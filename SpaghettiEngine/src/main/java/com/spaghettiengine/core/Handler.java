package com.spaghettiengine.core;

import org.lwjgl.glfw.GLFW;

import com.spaghettiengine.utils.*;

public final class Handler extends Thread {

	protected boolean stop;
	protected boolean stopOnNoActivity;
	protected FunctionDispatcher dispatcher;
	
	public Handler() {
		GLFW.glfwInit();
		dispatcher = new FunctionDispatcher();
		dispatcher.setDefaultId(getId());
	}
	
	@Override
	public void run() {
		while (!stop) {
			Utils.sleep(1);

			// This makes sure windows can be interacted with
			GameWindow.pollEvents();
			dispatcher.computeEvents();

			try {

				boolean found = false;
				for (Game game : Game.games) {
					// Detect soft-blocked instances and stop()
					if (!game.isStopped() && game.isDead()) {
						game.stop();
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
