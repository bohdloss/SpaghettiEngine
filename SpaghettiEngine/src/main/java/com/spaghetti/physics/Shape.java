package com.spaghetti.physics;

public class Shape {

	// Shape factory

	public static Shape getSquare() {
		return new Shape(new double[] { 0.5, -0.5, 0.5, -0.5 }, new double[] { 0.5, 0.5, -0.5, -0.5 });
	}

	protected double[] xcoords, ycoords;
	protected int point_count;

	public Shape(double[] xcoords, double[] ycoords) {
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

	public double[] getX() {
		return xcoords;
	}

	public double[] getY() {
		return ycoords;
	}

}