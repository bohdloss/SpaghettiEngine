package com.spaghettiengine.render;

import org.joml.Matrix4f;

import com.spaghettiengine.core.*;

public class Camera extends GameComponent {

	// Instace fields

	protected double scale;
	protected double yFov;
	protected double xFov;
	protected double targetRatio = 1.7777777777777777;

	protected Matrix4f projection;

	protected int width, height;

	public Camera(Level level, GameComponent parent, int width, int height) {
		super(level, parent);
		projection = new Matrix4f();
		setOrtho(width, height);
	}

	public void setOrtho(int width, int height) {
		projection.identity().setOrtho2D(-width / 2, width / 2, -height / 2, height / 2);
		this.width = width;
		this.height = height;
	}

	public void calcScale() {

		int win_w = width, win_h = height;

		double aspectRatio = (double) win_w / (double) win_h;

		double result = aspectRatio <= targetRatio ? win_w / xFov : (win_h / targetRatio) / yFov;

		scale = result;

	}

	public double getyFov() {
		return yFov;
	}

	public void setyFov(double yFov) {
		this.yFov = yFov;
	}

	public double getxFov() {
		return xFov;
	}

	public void setxFov(double xFov) {
		this.xFov = xFov;
	}

	public double getTargetRatio() {
		return targetRatio;
	}

	public void setTargetRatio(double targetRatio) {
		this.targetRatio = targetRatio;
	}

}
