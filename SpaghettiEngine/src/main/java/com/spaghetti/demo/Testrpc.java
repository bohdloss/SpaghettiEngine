package com.spaghetti.demo;

import com.spaghetti.world.GameObject;
import com.spaghetti.networking.RemoteProcedure;
import com.spaghetti.utils.Logger;

public class Testrpc extends RemoteProcedure {

	@Override
	protected Class<?>[] getArgumentTypes() {
		return new Class<?>[] { Integer.class };
	}

	@Override
	protected Class<?> getReturnType() {
		return Integer.class;
	}

	@Override
	protected Object onCall(Object[] args, GameObject player) throws Throwable {
		Integer arg0 = (Integer) args[0];
		Logger.info("Received number " + arg0);
		Logger.info("Sending number " + arg0 + " + 1 = " + (arg0 + 1));
		return arg0 + 1;
	}

}
