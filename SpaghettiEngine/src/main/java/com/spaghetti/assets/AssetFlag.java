package com.spaghetti.assets;

public class AssetFlag {

	public boolean needLoad;
	public boolean queued;
	public final String type;

	public AssetFlag(String type) {
		this.type = type;
	}
	
}
