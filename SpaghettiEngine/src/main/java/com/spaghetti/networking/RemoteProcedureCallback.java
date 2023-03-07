package com.spaghetti.networking;

import com.spaghetti.networking.RemoteProcedure;

public interface RemoteProcedureCallback {

	public void receiveReturnValue(RemoteProcedure rpc, Object returnvalue, boolean error, boolean ack);

}
