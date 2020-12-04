package com.spaghettiengine.components;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghettiengine.core.*;

public class Camera extends GameComponent {

	// Instace fields

	protected double scale;
	protected double fov = 10;
	protected double targetRatio = 1.7777777777777777;

	protected Matrix4d projection;
	protected Matrix4d cache;
	
	protected int width, height;
	
	// Cache
	private Vector3d vecC = new Vector3d();
	
	public Camera(Level level, GameComponent parent, int width, int height) {
		super(level, parent);
		projection = new Matrix4d();
		cache = new Matrix4d();
		setOrtho(width, height);
	}

	public void setOrtho(int width, int height) {
		// This makes sure depth testing works correctly for multi-layer rendering
		projection.identity().setOrtho(-width / 2, width / 2, -height / 2, height / 2, -1000, 1000);
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
	
	@Override
	public void onDestroy() {
		if(getLevel().getActiveCamera() == this) {
			getLevel().detachCamera();
		}
	}
	
	public Matrix4d getProjection() {
		getWorldPosition(vecC);
		cache.set(projection);
		cache.translate(-vecC.x, -vecC.y, 0);
		cache.scale(scale);
		return cache;
	}
	
}
