package com.spaghetti.core;

import com.spaghetti.input.Controller;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public abstract class CoreComponent extends Thread {

	private volatile Game source;
	private volatile FunctionDispatcher functionDispatcher;
	private volatile boolean stop;
	private volatile boolean init;
	private volatile boolean allowRun;
	private volatile boolean allowStop;
	private volatile boolean executionEnd;
	private volatile boolean requestChance;
	private volatile long lastTime;

	public final void initialize() throws Throwable {
		if (!validStarting()) {
			throw new IllegalStateException(
					"Error: attempted to initialize core thread state outside the context of a game");
		}
		try {
			functionDispatcher = new FunctionDispatcher();
			initialize0();
		} finally {
			init = true;
		}
	}

	protected abstract void initialize0() throws Throwable; // Your custom initialization code here!

	protected void postInitialize() throws Throwable {
	}

	public final void terminate() {
		if (!validStopping()) {
			throw new IllegalStateException("Error: attempted to stop core thread outside the context of a game");
		}
		stop = true;
	}

	protected abstract void terminate0() throws Throwable; // Your custom finalization code here!

	protected void preTerminate() throws Throwable {
	}

	public final void waitInit() {
		while (!initialized()) {
			Utils.sleep(1);
		}
	}

	public final void waitTerminate() {
		while (!stopped()) {
			Utils.sleep(1);
		}
	}

	public final void waitExecution() {
		while (!executionEnd) {
			Utils.sleep(1);
		}
	}

	public final void allowRun() {
		if (!validStarting()) {
			throw new IllegalStateException(
					"Error: attempted to modify core thread state outside the context of a game");
		}
		allowRun = true;
	}

	public final void allowStop() {
		if (!validStopping()) {
			throw new IllegalStateException(
					"Error: attempted to modify core thread state outside the context of a game");
		}
		allowStop = true;
	}

	@Override
	public final void start() {
		if (!validStarting()) {
			throw new IllegalStateException("Error: attempted to start core thread outside the context of a game");
		}
		super.start();
	}

	public final void start(Game source) {
		if (this.source != null && this.source != source) {
			throw new IllegalStateException("Error: attempted to start core multiple times in different games");
		}
		this.source = source;
		start();
	}

	@Override
	public final void run() {
		if (Thread.currentThread().getId() != this.getId()) {
			throw new IllegalStateException("Error: run() called but no new thread started");
		}

		// Initializer try catch
		try {
			initialize();
		} catch (Throwable t) {
			_uncaught(t);
			stop = true;
		}

		// Loop try catch
		try {
			while (!allowRun) {
				functionDispatcher.computeEvents();
				Utils.sleep(1);
			}
			postInitialize();
			while (!stop) {
				// Calculate delta
				long current = System.currentTimeMillis();
				if (lastTime == 0) {
					lastTime = current;
				}
				long pre = current - lastTime;
				float delta = pre;
				lastTime = current;

				// Compute queued operations
				functionDispatcher.computeEvents();
				loopEvents(delta);
				Utils.sleep(1);
			}
		} catch (Throwable t) {
			_uncaught(t);
		} finally {
			stop = true;
			source.stopAsync();
		}

		// Termination try catch
		try {
			preTerminate();
			executionEnd = true;
			while (!allowStop) {
				Utils.sleep(1);
				functionDispatcher.computeEvents();
			}
			terminate0();
		} catch (Throwable t) {
			_uncaught(t);
		}

		// Final chance to handle some requests
		requestChance = true;
		while (true) {
			boolean found = true;
			for (int i = 0; i < source.getComponentAmount(); i++) {
				if (!source.getComponentAt(i).requestChance) {
					found = false;
					break;
				}
			}
			if (found) {
				break;
			}
			Utils.sleep(1);
			functionDispatcher.computeEvents();
		}
	}

	private void _uncaught(Throwable t) {
		Logger.error("Fatal uncaught error in game " + source.getIndex() + ":", t);
	}

	protected abstract void loopEvents(float delta) throws Throwable; // Your custom loop code here!

	// Getters

	public final boolean stopped() {
		return stop && !isAlive();
	}

	public final boolean initialized() {
		return init;
	}

	public final boolean executionEnded() {
		return executionEnd;
	}

	protected abstract CoreComponent provideSelf();

	private final boolean validStarting() {
		return source != null && source.isStarting() && this == provideSelf();
	}

	private final boolean validStopping() {
		return source != null && source.isStopping() && this == provideSelf();
	}

	// Getters and setters

	public final Game getGame() {
		return source;
	}

	public final Level getLevel() {
		return source.getActiveLevel();
	}

	public final Camera getCamera() {
		return getGame().getActiveCamera();
	}

	public final Controller getController() {
		return getGame().getActiveController();
	}

	public final FunctionDispatcher getDispatcher() {
		return functionDispatcher;
	}

}
