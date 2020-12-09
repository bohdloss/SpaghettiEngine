package com.spaghettiengine.core;

import com.spaghettiengine.utils.*;

public abstract class CoreComponent extends Thread {

	protected Game source;
	private boolean stop;
	private boolean init;

	public CoreComponent(Game source) {
		this.source = source;
	}

	protected void init() {
		// Your custom initialization code here, then call super.init()
		init = true;
	}

	public final void terminate() {
		stop = true;
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

	@Override
	public final void run() {
		try {
			init();
			while (!stop) {
				Utils.sleep(1);
				loopEvents();
			}
		} catch (Throwable t) {
			System.out.println("Critical uncaught error in game " + source.getIndex() + ":");
			t.printStackTrace();
		} finally {
			stop = true;
		}
	}

	protected abstract void loopEvents(); // Your custom loop code here!

	// Getters

	public final boolean stopped() {
		return stop && !isAlive() && init;
	}

	public final boolean initialized() {
		return init;
	}

}
