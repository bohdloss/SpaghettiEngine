package com.spaghettiengine.core;

import com.spaghettiengine.utils.*;

public abstract class CoreComponent extends Thread {

	protected Game source;
	private boolean stop;
	private boolean init;
	private boolean allowRun;
	
	public CoreComponent(Game source) {
		this.source = source;
	}

	protected final void initialize() throws Throwable {
		try {
			initialize0();
		} finally {
			init = true;
		}
	}

	protected abstract void initialize0() throws Throwable; // Your custom initialization code here!

	public final void terminate() {
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

	public void allowRun() {
		allowRun = true;
	}
	
	@Override
	public final void run() {
		try {
			initialize();
			while(!allowRun) {
				Utils.sleep(1);
			}
			while (!stop) {
				Utils.sleep(1);
				loopEvents();
			}
			terminate0();
		} catch (Throwable t) {
			Logger.error("Critical uncaught error in game " + source.getIndex() + ":", t);
		} finally {
			stop = true;
		}
	}

	protected abstract void loopEvents() throws Throwable; // Your custom loop code here!

	// Getters

	public final boolean stopped() {
		return stop && !isAlive() && init;
	}

	public final boolean initialized() {
		return init;
	}

}
