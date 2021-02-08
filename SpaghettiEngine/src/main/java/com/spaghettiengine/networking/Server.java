package com.spaghettiengine.networking;

import com.spaghettiengine.core.*;
import com.spaghettiengine.utils.*;

public class Server extends CoreComponent {

	protected NetworkBuffer buffer;

	@Override
	protected void initialize0() throws Throwable {
		buffer = new NetworkBuffer(getSource().getOptions().getNetworkBufferSize());
	}

	@Override
	protected void terminate0() throws Throwable {
		buffer = null;
	}

	@Override
	protected void loopEvents(double delta) throws Throwable {
		Utils.sleep(1);
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getSource().getServer();
	}

}
