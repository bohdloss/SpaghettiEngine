package com.spaghetti.utils;

import java.awt.Dimension;
import java.awt.Toolkit;

import org.joml.Vector2i;

public final class GameOptions {

	protected Vector2i resolution;
	protected double tick;
	protected String assetSheetLocation;
	protected int networkBufferSize;

	public GameOptions() {

		findResolution();
		findTick();
		findAssetSheetLocation();
		findNetworkBufferSize();

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
		tick = 1;
	}

	private void findAssetSheetLocation() {
		assetSheetLocation = "/res/main.txt";
	}

	private void findNetworkBufferSize() {
		networkBufferSize = 1000 * 1000 * 10; // B * KB * MB = 10 MB
	}

	// Public getters and setters

	public void setTick(double tick) {
		this.tick = tick;
	}

	public double getTick() {
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

	public void setNetworkBufferSize(int size) {
		this.networkBufferSize = size;
	}

	public int getNetworkBufferSize() {
		return networkBufferSize;
	}

}