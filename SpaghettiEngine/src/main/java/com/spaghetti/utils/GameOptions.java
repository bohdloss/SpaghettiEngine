package com.spaghetti.utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;

import org.joml.Vector2i;

public final class GameOptions {

	private float tick;
	public static final String PREFIX = "spaghetti.";
	private HashMap<String, Object> options = new HashMap<>();

	public GameOptions() {

		findResolution();
		findTick();
		findStopTimeout();
		findAssetSheetLocation();
		findNetworkBufferSize();
		findPlayerClass();

	}

	private void findResolution() {
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

	private void findTick() {
		options.put(PREFIX + "tick", 1);
		tick = 1;
	}

	private void findStopTimeout() {
		options.put(PREFIX + "stoptimeout", 10000l); // 10 s
	}

	private void findAssetSheetLocation() {
		options.put(PREFIX + "assetsheet", "/res/main.txt");
	}

	private void findNetworkBufferSize() {
		options.put(PREFIX + "networkbuffer", 1000 * 1000 * 10); // B * KB * MB = 10 MB
	}

	private void findPlayerClass() {
		options.put(PREFIX + "playerclass", "com.spaghetti.objects.Player");
	}

	// Public getters and setters

	public void setTick(float tick) {
		options.put(PREFIX + "tick", tick);
		this.tick = tick;
	}

	public float getTick() {
		return tick;
	}

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