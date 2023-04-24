package com.spaghetti.dispatcher;

public interface VoidFunction extends Function {

	public default Object execute() throws Throwable{
		execute0();
		return null;
	}

	public abstract void execute0() throws Throwable;

}
