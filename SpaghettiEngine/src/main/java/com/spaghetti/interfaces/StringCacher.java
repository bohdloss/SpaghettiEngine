package com.spaghetti.interfaces;

public interface StringCacher {

	public abstract void cacheString(short hash, String string);
	public abstract String getCachedString(short hash);
	
	public abstract boolean containsString(short hash);
	
}
