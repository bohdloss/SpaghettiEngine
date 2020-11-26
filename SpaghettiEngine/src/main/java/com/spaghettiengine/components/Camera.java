package com.spaghettiengine.components;

import org.joml.Matrix4d;
import org.joml.Vector2d;

import com.spaghettiengine.core.*;

public class Camera extends GameComponent {

	// Instace fields

	protected double scale;
	protected double fov = 10;
	protected double targetRatio = 1.7777777777777777;

	protected Matrix4d projection;
	protected Matrix4d cache;
	protected Vector2d position;
	
	protected int width, height;
	
	public Camera(Level level, GameComponent parent, int width, int height) {
		super(level, parent);
		projection = new Matrix4d();
		cache = new Matrix4d();
		position = new Vector2d();
		setOrtho(width, height);
	}

	public void setOrtho(int width, int height) {
		projection.identity().setOrtho2D(-width / 2, width / 2, -height / 2, height / 2);
		this.width = width;
		this.height = height;
		calcScale();
	}

	public void calcScale() {
		int usedVal = min((int)((double)width/targetRatio), height);
		scale = usedVal / fov;
	}

	private int min(int a, int b) {
		return a < b ? a : b;
	}
	
	public double getFov() {
		return fov;
	}

	public void setFov(double fov) {
		this.fov = fov;
		calcScale();
	}
	
	public double getTargetRatio() {
		return targetRatio;
	}

	public void setTargetRatio(double targetRatio) {
		this.targetRatio = targetRatio;
		calcScale();
	}

	public void setX(double x) {
		position.x = x;
	}
	
	public void setY(double y) {
		position.y = y;
	}
	
	public double getX() {
		return position.x;
	}
	
	public double getY() {
		return position.y;
	}
	
	@Override
	public void onDestroy() {
		if(getLevel().getActiveCamera() == this) {
			getLevel().detachCamera();
		}
	}
	
	public Matrix4d getProjection() {
		cache.set(projection);
		cache.translate(position.x, position.y, 0);
		cache.scale(scale);
		return cache;
	}
	
}
