package com.spaghetti.utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;

import org.joml.Vector2i;

import com.spaghetti.core.Game;

public class GameOptions {

	protected final Game game;

	public static final String PREFIX = "spaghetti.";
	protected HashMap<String, Object> options = new HashMap<>();

	public GameOptions(Game game) {
		this.game = game;

		findResolution();
		findStopTimeout();
		findAssetSheetLocation();
		findNetworkBufferSize();
	}

	protected void findResolution() {
		Vector2i resolution = new Vector2i();

		if ("true".equals(System.getProperty("java.awt.headless"))) {
			return;
		}

		try {

			Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
			resolution.x = dimension.width;
			resolution.y = dimension.height;

		} catch (Throwable t) {
		}

		options.put(PREFIX + "resolution", resolution);

	}

	protected void findStopTimeout() {
		options.put(PREFIX + "stoptimeout", 10000l); // 10 s
	}

	protected void findAssetSheetLocation() {
		options.put(PREFIX + "assetsheet", "/res/main.txt");
	}

	protected void findNetworkBufferSize() {
		options.put(PREFIX + "networkbuffer", 1000 * 1000 * 10); // B * KB * MB = 10 MB
	}

	// Public getters and setters

	public void setOption(String name, Object value) {
		if (name.startsWith(PREFIX) && !options.containsKey(name)) {
			throw new IllegalArgumentException();
		}
		options.put(name, value);
	}

	public Object ngetOption(String name) {
		Object get = options.get(name);
		if (get == null) {
			Logger.warning("Non-existant game option \"" + name + "\" requested");
		}
		return get;
	}

	@SuppressWarnings("unchecked")
	public <T> T getOption(String name) {
		return (T) ngetOption(name);
	}

}