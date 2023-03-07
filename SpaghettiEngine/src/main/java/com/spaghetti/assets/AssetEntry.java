package com.spaghetti.assets;

public class AssetEntry {

	public AssetEntry(AssetManager owner) {
		this.owner = owner;
	}

	// Owning class
	public final AssetManager owner;

	// Static data
	public String type;
	public String name;
	public String[] args;

	// Runtime data
	public Asset asset;
	public boolean loading;
	public boolean unloading;

	public boolean isBusy() {
		return loading || unloading;
	}

}
