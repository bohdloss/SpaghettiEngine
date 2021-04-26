package com.spaghetti.objects;

import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.spaghetti.core.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.render.*;
import com.spaghetti.utils.*;

public class Camera extends GameObject {

	// Instance fields

	protected float scale;
	protected float fov = 10;
	protected float targetRatio = 16 / 9; // 16:9 resolution

	protected Matrix4f projection = new Matrix4f();
	protected Matrix4f cache = new Matrix4f();
	protected Matrix4f view = new Matrix4f();

	protected int width, height;
	protected boolean clearColor = true, clearDepth = true, clearStencil = true;

	protected FrameBuffer renderTarget;

	protected void setProjectionMatrix() {
		projection.identity().setOrtho(-width / 2, width / 2, -height / 2, height / 2, -1000, 1000);
		calcScale();
	}

	public void calcScale() {
		float usedVal = CMath.min(width / targetRatio, height);
		scale = (usedVal / fov) * 2;
	}

	public float getFov() {
		return fov;
	}

	public void setFov(float fov) {
		this.fov = fov;
		calcScale();
	}

	public float getTargetRatio() {
		return targetRatio;
	}

	public void setTargetRatio(float targetRatio) {
		this.targetRatio = targetRatio;
		calcScale();
	}

	@Override
	public void onDestroy() {
		if (getLevel().getActiveCamera() == this) {
			getLevel().detachCamera();
		}
		if (!getGame().isHeadless()) {
			if (!getGame().getRenderer().isAlive()) {
				throw new IllegalStateException("Can't delete framebuffer: RENDERER died");
			}
			getGame().getRendererDispatcher().quickQueue(() -> {
				renderTarget.delete();
				return null;
			});
		}
	}

	public Matrix4f getProjection() {
		Vector3f vecC = new Vector3f();
		getWorldPosition(vecC);
		cache.set(projection);
		cache.scale(scale, scale, 1);
		cache.translate(-vecC.x, -vecC.y, -vecC.z);
		return cache;
	}

	public Matrix4f getUntransformedProjection() {
		cache.set(projection);
		cache.scale(scale, scale, 1);
		return cache;
	}

	protected void checkTarget() {
		if (renderTarget == null) {
			Vector2i res = getGame().getOptions().getOption(GameOptions.PREFIX + "resolution");
			renderTarget = new FrameBuffer(res.x, res.y);
			updateValues();
		}
	}

	protected void updateValues() {
		width = renderTarget.getWidth();
		height = renderTarget.getHeight();
		targetRatio = ((float) width) / ((float) height);
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

	public float getCameraScale() {
		return scale;
	}

	public float getWidth() {
		return width;
	}

	public float getHeight() {
		return height;
	}

	@Override
	public void serverUpdate(float delta) {

	}

	@Override
	public void clientUpdate(float delta) {

	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		super.readDataClient(buffer);
		fov = buffer.getFloat();
		clearColor = buffer.getBoolean();
		clearDepth = buffer.getBoolean();
		clearStencil = buffer.getBoolean();
		calcScale();
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		super.writeDataServer(buffer);
		buffer.putFloat(fov);
		buffer.putBoolean(clearColor);
		buffer.putBoolean(clearDepth);
		buffer.putBoolean(clearStencil);
	}

	@Override
	public void render(Matrix4f projection, float delta) {
		checkTarget();
		renderTarget.use();
		if (getClearColor()) {
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
		}
		if (getClearDepth()) {
			GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
		}
		if (getClearStencil()) {
			GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
		}

		Matrix4f sceneMatrix = new Matrix4f();
		Vector3f vec3cache = new Vector3f();
		getLevel().forEachActualObject((id, component) -> {
			if (!Camera.class.isAssignableFrom(component.getClass())) {

				// Reset matrix
				sceneMatrix.set(getProjection());

				// Get world position
				component.getWorldPosition(vec3cache);
				sceneMatrix.translate(vec3cache);

				// Get world rotation
				component.getWorldRotation(vec3cache);
				sceneMatrix.rotateXYZ(vec3cache);

				// Get world scale
				component.getWorldScale(vec3cache);
				sceneMatrix.scale(vec3cache.x, vec3cache.y, 1);

				component.render(sceneMatrix, delta);
			}
		});
		renderTarget.stop();
	}

}
