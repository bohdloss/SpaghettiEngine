package com.spaghetti.core;

import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public abstract class CoreComponent extends Thread {
	
	private Game source;
	private FunctionDispatcher functionDispatcher;
	private boolean stop;
	private boolean init;
	private boolean allowRun;
	private boolean executionEnd;
	private long lastTime;

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

	public final void terminate() {
		if (!validStopping()) {
			throw new IllegalStateException("Error: attempted to stop core thread outside the context of a game");
		}
		stop = true;
	}

	protected abstract void terminate0() throws Throwable; // Your custom finalization code here!

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
		while(!executionEnd) {
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

	@Override
	public final void start() {
		if (!validStarting()) {
			throw new IllegalStateException("Error: attempted to start core thread outside the context of a game");
		}
		super.start();
	}

	public final void start(Game source) {
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
		} catch(Throwable t) {
			_uncaught(t);
			stop = true;
		}
		
		// Loop try catch
		try {
			while (!allowRun) {
				Utils.sleep(1);
			}
			while (!stop) {
				// Calculate delta
				long current = System.currentTimeMillis();
				if (lastTime == 0) {
					lastTime = current;
				}
				long pre = current - lastTime;
				double delta = pre;
				lastTime = current;

				// Compute queued operations
				functionDispatcher.computeEvents();
				loopEvents(delta);
			}
		} catch (Throwable t) {
			_uncaught(t);
		} finally {
			stop = true;
			// This means that not only this component has received the shutdown signal
			// but has already exited out of the loop
			executionEnd = true;
		}
		
		// Termination try catch
		try {
			terminate0();
		} catch (Throwable t) {
			_uncaught(t);
		}
	}

	private void _uncaught(Throwable t) {
		Logger.error("Fatal uncaught error in game " + source.getIndex() + ":", t);
	}
	
	protected abstract void loopEvents(double delta) throws Throwable; // Your custom loop code here!

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
		Level level;
		if ((level = getLevel()) == null) {
			return null;
		}
		return level.getActiveCamera();
	}

	public final FunctionDispatcher getDispatcher() {
		return functionDispatcher;
	}
	
}