package com.spaghettiengine.core;

public class Updater extends CoreComponent {

	public Updater(Game source) {
		super(source);
	}

	@Override
	protected void loopEvents() {
		if (source.getActiveLevel() != null) {
			source.getActiveLevel().update(0);
		}
	}

}
