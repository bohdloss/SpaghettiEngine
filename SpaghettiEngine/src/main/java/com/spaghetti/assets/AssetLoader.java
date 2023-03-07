package com.spaghetti.assets;

public interface AssetLoader<T extends Asset> {

	public T instantiate();

	public String[] getDependencies(String[] args);

	public Object[] load(String[] args) throws Throwable;

	public String[] getDefaultArgs();

}
