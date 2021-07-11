package com.spaghetti.assets;

public class SheetEntry {

	public SheetEntry(AssetManager owner) {
		this.owner = owner;
	}

	// Final pointer to assetmanager
	public final AssetManager owner;

	// Static data
	public String type;
	public String name;
	public String[] args;

	// Runtime data
	public Asset asset;
	public boolean loading;
	public boolean unloading;
	public boolean resetting;

	public boolean isBusy() {
		return loading || unloading || resetting;
	}

}
