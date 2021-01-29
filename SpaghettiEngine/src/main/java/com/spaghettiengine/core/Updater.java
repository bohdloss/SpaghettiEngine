package com.spaghettiengine.core;

public class Updater extends CoreComponent {

	public Updater(Game source) {
		super(source);
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		source.dispatcher.computeEvents();
		
		source.getWindow().getInputDispatcher().update();
		
		if (source.getActiveLevel() != null) {
			source.getActiveLevel().update(delta);
		}
	}

	@Override
	protected void initialize0() throws Throwable {

	}

	@Override
	protected void terminate0() throws Throwable {
		if (source.getActiveLevel() != null) {
			source.getActiveLevel().destroy();
		}
	}

}
