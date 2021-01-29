package com.spaghettiengine.physics;

import org.joml.Rectangled;

public class Shape {
	
	// Shape factory
	
	public static Shape getSquare() {
		return new Shape(
				new double[] {
						0.5,
						-0.5,
						0.5,
						-0.5
				},
				new double[] {
						0.5,
						0.5,
						-0.5,
						-0.5
				});
	}
	
	protected double[] xcoords, ycoords;
	protected int point_count;
	protected Rectangled bounds = new Rectangled();
	
	public Shape(double[] xcoords, double[] ycoords) {
		this.xcoords = xcoords;
		this.ycoords = ycoords;
		if(xcoords.length != ycoords.length) {
			throw new IllegalArgumentException();
		}
		this.point_count = xcoords.length;
		
		// Find bounds
		
		for(int i = 0; i < point_count; i++) {
			double xx = xcoords[i], yy = ycoords[i];
			
			if(xx < bounds.minX) {
				bounds.minX = xx;
			}
			if(xx > bounds.maxX) {
				bounds.maxX = xx;
			}
			if(yy < bounds.minY) {
				bounds.minY = yy;
			}
			if(yy > bounds.maxY) {
				bounds.maxY = yy;
			}
		}
		
	}
	
	// Getters
	
	public Rectangled getBounds() {
		return bounds;
	}
	
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