package com.spaghetti.demo;

import com.spaghetti.interfaces.*;
import com.spaghetti.networking.*;
import com.spaghetti.utils.Logger;

@ToClient
public class Testrpc extends RPC {

	@Override
	protected ClassInterpreter<?>[] getArgInterpreters() {
		return new ClassInterpreter<?>[] { DefaultInterpreters.interpreters.get("int") };
	}

	@Override
	protected ClassInterpreter<?> getReturnInterpreter() {
		return DefaultInterpreters.interpreters.get("int");
	}

	@Override
	protected boolean hasResponse() {
		return true;
	}

	@Override
	protected Object execute0(boolean isClient, NetworkConnection worker, Object[] args) throws Throwable {
		Integer arg0 = (Integer) args[0];
		Logger.info("Received number " + arg0);
		Logger.info("Sending number " + arg0 + " + 1 = " + (arg0 + 1));
		return arg0 + 1;
	}

}
