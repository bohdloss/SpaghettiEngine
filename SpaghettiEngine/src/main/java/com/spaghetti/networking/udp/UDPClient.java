package com.spaghetti.networking.udp;

import com.spaghetti.networking.ClientCore;

public class UDPClient extends ClientCore {

	@Override
	public void initialize0() throws Throwable {
		super.initialize0();
		this.worker = new UDPConnection(this);
	}

}
