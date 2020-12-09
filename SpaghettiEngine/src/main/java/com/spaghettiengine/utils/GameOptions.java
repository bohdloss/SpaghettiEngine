package com.spaghettiengine.utils;

import java.awt.Dimension;
import java.awt.Toolkit;

import org.joml.Vector2i;

public final class GameOptions {

	protected Vector2i resolution = new Vector2i(0, 0);
	protected long tick = 25;

	public GameOptions() {

		findResolution();

	}

	private void findResolution() {
		if ("true".equals(System.getProperty("java.awt.headless"))) {
			return;
		}

		try {

			Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
			resolution.x = dimension.width;
			resolution.y = dimension.height;

		} catch (Throwable t) {
		}

	}

	// Public getters and setters

	public void setTick(long tick) {
		this.tick = tick;
	}

	public long getTick() {
		return tick;
	}

	public void setResolution(int x, int y) {
		resolution.x = x;
		resolution.y = y;
	}

	public Vector2i getResolution() {
		return resolution;
	}

}
