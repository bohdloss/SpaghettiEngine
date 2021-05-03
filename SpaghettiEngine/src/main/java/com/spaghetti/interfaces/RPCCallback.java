package com.spaghetti.interfaces;

import com.spaghetti.networking.RPC;

public interface RPCCallback {

	public void receiveReturnValue(RPC rpc, Object returnvalue, boolean error, boolean ack);

}
