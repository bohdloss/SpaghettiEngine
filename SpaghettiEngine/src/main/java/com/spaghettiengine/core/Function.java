package com.spaghettiengine.core;

import java.util.Random;

public abstract class Function <T>{

	private static Random random = new Random();
	private long id;

	public Function() {
	this.id=random.nextLong();
	}
	
	public abstract T execute() throws Throwable;

	public long getId() {
		return id;
	}

}
