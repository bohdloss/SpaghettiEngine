package com.spaghettiengine.core;

public class Updater extends CoreComponent {

	public Updater(Game source) {
		super(source);
	}

	@Override
	protected void loopEvents() throws Throwable {
		source.dispatcher.computeEvents();

		if (source.getActiveLevel() != null) {
			source.getActiveLevel().update(0);
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void terminate0() throws Throwable {
		if(source.getActiveLevel() != null) {
			source.getActiveLevel().destroy();
		}
	}

}
