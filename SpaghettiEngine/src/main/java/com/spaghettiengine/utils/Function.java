package com.spaghettiengine.utils;

import java.util.Random;

public final class Function {

	private static final Random random = new Random();
	private final long id;
	private final FuncAction action;
	protected long thread;

	public Function(FuncAction action) {
		this.id = random.nextLong();
		this.action = action;
	}

	public Object execute() throws Throwable {
		if (action != null) {
			return action.execute();
		} else {
			return null;
		}
	}

	public final long getId() {
		return id;
	}

}
