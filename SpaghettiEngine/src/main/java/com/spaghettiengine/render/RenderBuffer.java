package com.spaghettiengine.render;

import org.lwjgl.opengl.GL30;

import com.spaghettiengine.assets.Asset;

public class RenderBuffer extends Asset {

	public static final int COLOR = GL30.GL_RGBA;
	public static final int DEPTH = GL30.GL_DEPTH_COMPONENT;
	public static final int STENCIL = GL30.GL_STENCIL_INDEX8;

	protected int id;
	protected int width, height;
	protected int type;

	public RenderBuffer(int width, int height, int type) {
		setData(width, height, type);
		load();
	}

	public RenderBuffer() {
	}

	@Override
	public void setData(Object... objects) {
		if (valid()) {
			return;
		}

		width = (int) objects[0];
		height = (int) objects[1];
		type = (int) objects[2];
	}

	@Override
	public boolean isFilled() {
		return (type == COLOR || type == DEPTH || type == STENCIL) && height > 0 && width > 0;
	}

	protected void _use() {
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, id);
	}

	@Override
	protected void load0() {

		// Generate valid id

		id = GL30.glGenRenderbuffers();

		// Bind buffer

		_use();

		// Allocate buffer storage

		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, type, width, height);

	}

	public int getId() {
		return id;
	}

	public int getType() {
		return type;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	protected void delete0() {
		GL30.glDeleteRenderbuffers(id);
	}

	@Override
	protected void reset0() {
		id = -1;
		width = 0;
		height = 0;
		type = -1;
	}

}
