package com.spaghettiengine.networking;

import com.spaghettiengine.core.*;

public class Client extends CoreComponent {

	@Override
	protected void initialize0() throws Throwable {
		
	}

	@Override
	protected void terminate0() throws Throwable {
		
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getSource().getClient();
	}
	
}
