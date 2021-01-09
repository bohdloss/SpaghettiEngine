package com.spaghettiengine.utils;

import java.awt.Dimension;
import java.awt.Toolkit;

import org.joml.Vector2i;

public final class GameOptions {

	protected Vector2i resolution;
	protected long tick;
	protected String assetSheetLocation;

	public GameOptions() {

		findResolution();
		findTick();
		findAssetSheetLocation();

	}

	private void findResolution() {
		resolution = new Vector2i();

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

	private void findTick() {
		tick = 25;
	}

	private void findAssetSheetLocation() {
		assetSheetLocation = "/res/main.txt";
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

	public void setAssetSheetLocation(String assetSheetLocation) {
		this.assetSheetLocation = assetSheetLocation;
	}

	public String getAssetSheetLocation() {
		return assetSheetLocation;
	}

}
