package com.spaghettiengine.components;

import org.joml.Matrix4d;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL11;

import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.*;

public class Camera extends GameComponent {

	// Instace fields

	protected double scale;
	protected double fov = 10;
	protected double targetRatio = 1.7777777777777777;

	protected Matrix4d projection = new Matrix4d();
	protected Matrix4d cache = new Matrix4d();

	protected int width, height;

	protected boolean clearColor = true, clearDepth = true;

	protected FrameBuffer renderTarget;

	// Cache
	private Vector3d vecC = new Vector3d();

	public Camera(Level level, GameComponent parent, int width, int height) {
		super(level, parent);
		setOrtho(width, height);
	}

	public Camera(Level level, GameComponent parent) {
		super(level, parent);
	}

	public void setOrtho(int width, int height) {
		// This makes sure depth testing works correctly for multi-layer rendering
		projection.identity().setOrtho(-width / 2, width / 2, -height / 2, height / 2, -1000, 1000);
		this.width = width;
		this.height = height;
		calcScale();
	}

	public void calcScale() {
		int usedVal = min((int) (width / targetRatio), height);
		scale = usedVal / fov;
	}

	private final int min(int a, int b) {
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
		if (getLevel().getActiveCamera() == this) {
			getLevel().detachCamera();
		}
	}

	public Matrix4d getProjection() {
		getWorldPosition(vecC);
		cache.set(projection);
		cache.translate(-vecC.x, -vecC.y, 0);
		cache.scale(scale, scale, 1);
		return cache;
	}

	private void checkTarget() {
		if (renderTarget == null) {
			Vector2i res = getGame().getOptions().getResolution();
			renderTarget = new TextureFrameBuffer(res.x, res.y);
		}
	}
	
	public void draw() {
		checkTarget();

		renderTarget.use();

		if (clearColor) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
		if (clearDepth) {
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		}

		getLevel().render(getProjection());

		FrameBuffer.stop();

	}

	// Interfaces

	@Override
	public void getReplicateData(SpaghettiBuffer buffer) {

		super.getReplicateData(buffer);

		buffer.putDouble(fov);
		buffer.putDouble(targetRatio);

		buffer.putInt(width);
		buffer.putInt(height);

		buffer.putBoolean(clearColor);
		buffer.putBoolean(clearDepth);

	}

	@Override
	public void setReplicateData(SpaghettiBuffer buffer) {

		super.setReplicateData(buffer);

		fov = buffer.getDouble();
		targetRatio = buffer.getDouble();

		width = buffer.getInt();
		height = buffer.getInt();

		clearColor = buffer.getBoolean();
		clearDepth = buffer.getBoolean();

		setOrtho(width, height);

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

	public FrameBuffer getFrameBuffer() {
		checkTarget();
		return renderTarget;
	}

	public void setFrameBuffer(FrameBuffer renderTarget) {
		this.renderTarget = renderTarget;
	}

	@Override
	public void serverUpdate(double delta) {
		
	}

	@Override
	public void clientUpdate(double delta) {
		
	}

	@Override
	public void renderUpdate() {
		
	}
	
}
