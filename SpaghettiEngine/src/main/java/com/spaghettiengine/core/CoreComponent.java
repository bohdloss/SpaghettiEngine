package com.spaghettiengine.core;

import com.spaghettiengine.objects.Camera;
import com.spaghettiengine.utils.*;

public abstract class CoreComponent extends Thread {

	private Game source;
	private boolean stop;
	private boolean init;
	private boolean allowRun;
	private long lastTime;

	public final void initialize() throws Throwable {
		if(!validStarting()) {
			throw new IllegalStateException("Error: attempted to initialize core thread state outside the context of a game");
		}
		try {
			initialize0();
		} finally {
			init = true;
		}
	}

	protected abstract void initialize0() throws Throwable; // Your custom initialization code here!

	public final void terminate() {
		if(!validStopping()) {
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

	public final void allowRun() {
		if(!validStarting()) {
			throw new IllegalStateException("Error: attempted to modify core thread state outside the context of a game");
		}
		allowRun = true;
	}

	@Override
	public final void start() {
		if(!validStarting()) {
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
		if(Thread.currentThread().getId() != this.getId()) {
			throw new IllegalStateException("Error: run() called but no new thread started");
		}
		try {
			initialize();
			while (!allowRun) {
				Utils.sleep(1);
			}
			while (!stop) {
				long current = System.currentTimeMillis();

				if (lastTime == 0) {
					lastTime = current;
				}

				long time = System.currentTimeMillis();

				long pre = time - lastTime;
				double delta = pre;

				lastTime = current;

				loopEvents(delta);
			}
			// Known bug: for some reason (probably synchronization)
			// this code hangs right there in certain circumstances
			terminate0();
		} catch (Throwable t) {
			Logger.error("Fatal uncaught error in game " + source.getIndex() + ":", t);
		} finally {
			stop = true;
		}
	}

	protected abstract void loopEvents(double delta) throws Throwable; // Your custom loop code here!

	// Getters

	public final boolean stopped() {
		return stop && !isAlive() && init;
	}

	public final boolean initialized() {
		return init;
	}

	protected abstract CoreComponent provideSelf();
	
	private final boolean validStarting() {
		return source != null && source.isStarting() && this == provideSelf();
	}
	
	private final boolean validStopping() {
		return source != null && source.isStopping() && this == provideSelf();
	}
	
	// Getters and setters

	protected final Game getSource() {
		return source;
	}
	
	protected final Level getLevel() {
		return source.getActiveLevel();
	}

	protected final Camera getCamera() {
		if (getLevel() == null) {
			return null;
		}
		return getLevel().getActiveCamera();
	}

}
