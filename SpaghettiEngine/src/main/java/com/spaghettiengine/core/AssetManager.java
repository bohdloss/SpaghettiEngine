package com.spaghettiengine.core;

import java.util.HashMap;

import com.spaghettiengine.render.*;

public class AssetManager {

	// Always better keep a reference to game
	protected Game source;

	// Cache asset that are being actively used
	private HashMap<String, Model> modelCache = new HashMap<>();

	public AssetManager(Game source) {
		this.source = source;
	}

	public Model getModel(String name) {

		// TODO

		return modelCache.get(name);

	}

}
