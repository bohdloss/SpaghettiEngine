package com.spaghetti.render;

import com.spaghetti.networking.ConnectionManager;
import org.joml.Matrix4f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.spaghetti.world.GameObject;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.MathUtil;
import com.spaghetti.utils.Transform;

public class Camera extends GameObject {

	// Variables
	protected float scale;
	protected float fov;
	protected float targetRatio = 16f / 9f; // 16:9 resolution
	protected boolean usePosition = true, useRotation;

	// Cache
	protected Matrix4f projection = new Matrix4f();
	protected Matrix4f cache = new Matrix4f();
	protected Matrix4f view = new Matrix4f();
	protected Matrix4f precalculated = new Matrix4f();

	// OpenGL data
	protected int width, height;
	protected boolean clearColor = true, clearDepth = true, clearStencil = true;
	protected FrameBuffer renderTarget;
	protected boolean rendering;

	public Camera() {
		fov = 10;
		setVisible(true);
	}

	protected void setProjectionMatrix() {
		projection.identity().setOrtho(-width / 2, width / 2, -height / 2, height / 2, -1000, 1000);
		calcScale();
	}

	public void calcScale() {
		float usedVal = MathUtil.min(width / targetRatio, height);
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
		if (getGame().getLocalCamera() == this) {
			getGame().setLocalCamera(null);
		}
		if (!getGame().isHeadless()) {
			getGame().getPrimaryDispatcher().queueVoid(renderTarget::unload, true);
		}
	}

	protected void onBeginFrame(Transform transform) {
		rendering = true;
		precalculated.set(projection);
		precalculated.scale(scale, scale, 1);

		if(useRotation) {
			Vector3f rot = transform.rotation;
			precalculated.rotateXYZ(-rot.x, -rot.y, -rot.z);
		}
		if(usePosition) {
			Vector3f pos = transform.position;
			precalculated.translate(-pos.x, -pos.y, -pos.z);
		}
	}

	protected void onEndFrame(Transform transform) {
		rendering = false;
	}

	public Matrix4f getProjection() {
		cache.set(precalculated);
		return cache;
	}

	public Matrix4f getUntransformedProjection() {
		cache.set(projection);
		cache.scale(scale, scale, 1);
		return cache;
	}

	protected void checkTarget() {
		if (renderTarget == null) {
			Vector2i res = getGame().getEngineSetting("render.resolution");
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
	public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		super.readDataClient(manager, buffer);
		fov = buffer.getFloat();

		byte flags = buffer.getByte();

		clearColor = (flags & 1) == 1;
		clearDepth = (flags & (1 << 1)) == 1;
		clearStencil = (flags & (1 << 2)) == 1;
		usePosition = (flags & (1 << 3)) == 1;
		useRotation = (flags & (1 << 4)) == 1;

		calcScale();
	}

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		super.writeDataServer(manager, buffer);
		buffer.putFloat(fov);

		byte flags = 0;
		flags |= (clearColor ? 1 : 0);
		flags |= (clearDepth ? 1 : 0) << 1;
		flags |= (clearStencil ? 1 : 0) << 2;
		flags |= (usePosition ? 1 : 0) << 3;
		flags |= (useRotation ? 1 : 0) << 4;

		buffer.putByte(flags);
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
		// Prevent recursion
		if(rendering) {
			return;
		}

		// Prepare buffer
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

		// Pre-frame trigger
		onBeginFrame(transform);

		// Render frame
		getLevel().forEachObject(object -> {
			if (object != this) {
				object.render(this, delta);
			}
		});

		// Post-frame trigger
		onEndFrame(transform);

		renderTarget.stop();
	}

}
