package com.spaghettiengine.core;

public class Updater extends Thread implements Tickable {

	protected Game source;
	
	public Updater(Game source) {
		this.source=source;
	}
	
	@Override
	public void update(float delta) {
		
	}

}
