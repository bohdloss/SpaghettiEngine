package com.spaghetti.physics;

public class Shape {

	// Shape factory

	public static Shape getSquare() {
		return new Shape(new float[] { 0.5f, -0.5f, 0.5f, -0.5f }, new float[] { 0.5f, 0.5f, -0.5f, -0.5f });
	}

	protected float[] xcoords, ycoords;
	protected int point_count;

	public Shape(float[] xcoords, float[] ycoords) {
		this.xcoords = xcoords;
		this.ycoords = ycoords;
		if (xcoords.length != ycoords.length) {
			throw new IllegalArgumentException();
		}
		this.point_count = xcoords.length;

	}

	// Getters

	public int getPointCount() {
		return point_count;
	}

	public float[] getX() {
		return xcoords;
	}

	public float[] getY() {
		return ycoords;
	}

}