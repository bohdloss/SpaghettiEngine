package com.spaghetti.objects;

import org.joml.Matrix4d;
import org.joml.Vector2i;
import org.joml.Vector3d;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.Replicate;
import com.spaghetti.render.*;
import com.spaghetti.utils.*;

public class Camera extends GameObject {

	// Instace m_fields
	
	protected double scale;
	@Replicate
	protected double fov = 10;
	protected double targetRatio = 1.7777777777777777; // 16:9

	protected Matrix4d projection = new Matrix4d();
	protected Matrix4d cache = new Matrix4d();
	protected Matrix4d view = new Matrix4d();

	protected int width, height;
	@Replicate
	protected boolean clearColor = true, clearDepth = true, clearStencil = true;

	protected FrameBuffer renderTarget;

	// Cache
	private Vector3d vecC = new Vector3d();

	public Camera(Level level, GameObject parent) {
		super(level, parent);
	}

	protected void setProjectionMatrix() {
		projection.identity().setOrtho(-width / 2, width / 2, -height / 2, height / 2, -1000, 1000);
		calcScale();
	}

	public void calcScale() {
		double usedVal = CMath.min(width / targetRatio, height);
		scale = (usedVal / fov) * 2;
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
		if (getLevel().getActiveCamera() == this) {
			getLevel().detachCamera();
		}
	}

	public Matrix4d getProjection() {
		getWorldPosition(vecC);
		cache.set(projection);
		cache.scale(scale, scale, 1);
		cache.translate(-vecC.x, -vecC.y, -vecC.z);
		return cache;
	}

	protected void checkTarget() {
		if (renderTarget == null) {
			Vector2i res = getGame().getOptions().getResolution();
			renderTarget = new FrameBuffer(res.x, res.y);
			updateValues();
		}
	}

	protected void updateValues() {
		width = renderTarget.getWidth();
		height = renderTarget.getHeight();
		targetRatio = ((double) width) / ((double) height);
		setProjectionMatrix();
	}
	
	// Getters and setters

	public boolean getClearColor() {
		return clearColor;
	}

	public void setClearColor(boolean clearColor) {
		this.clearColor = clearColor;
	}

	public boolean getClearDepth() {
		return clearDepth;
	}

	public void setClearDepth(boolean clearDepth) {
		this.clearDepth = clearDepth;
	}

	public boolean getClearStencil() {
		return clearStencil;
	}

	public void setClearStencil(boolean clearStencil) {
		this.clearStencil = clearStencil;
	}

	public FrameBuffer getFrameBuffer() {
		checkTarget();
		return renderTarget;
	}

	public void setFrameBuffer(FrameBuffer renderTarget) {
		this.renderTarget = renderTarget;
	}

	public double getCameraScale() {
		return scale;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	@Override
	public void serverUpdate(double delta) {

	}

	@Override
	public void clientUpdate(double delta) {

	}

	@Override
	public void render(Matrix4d projection, double delta) {

	}

}
