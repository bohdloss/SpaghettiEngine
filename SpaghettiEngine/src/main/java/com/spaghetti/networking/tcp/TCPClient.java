package com.spaghetti.networking.tcp;

import com.spaghetti.networking.ClientCore;

public class TCPClient extends ClientCore {

	@Override
	public void initialize0() throws Throwable {
		super.initialize0();
		this.worker = new TCPConnection(this);
	}
	
}
