package com.spaghetti.render;

import com.spaghetti.utils.ExceptionUtil;
import org.lwjgl.opengl.GL30;

import com.spaghetti.assets.Asset;

public class RenderBuffer extends Asset {

	public static final int COLOR = GL30.GL_RGBA;
	public static final int DEPTH = GL30.GL_DEPTH_COMPONENT;

	protected int id;
	protected int width, height;
	protected int type;

	public RenderBuffer(int width, int height, int type) {
		setData(new Object[] {width, height, type});
		load();
	}

	public RenderBuffer() {
	}

	@Override
	public void setData(Object[] objects) {
		if (isLoaded()) {
			return;
		}

		width = (int) objects[0];
		height = (int) objects[1];
		type = (int) objects[2];
	}

	@Override
	public boolean isFilled() {
		return (type == COLOR || type == DEPTH) && height > 0 && width > 0;
	}

	protected void _use() {
		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, id);
	}

	@Override
	protected void load0() {
		ExceptionUtil.glConsumeError();
		// Generate valid id

		id = GL30.glGenRenderbuffers();
		ExceptionUtil.glError();

		// Bind w_buffer

		_use();

		// Allocate w_buffer storage

		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, type, width, height);
		ExceptionUtil.glError();

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
	protected void unload0() {
		ExceptionUtil.glConsumeError();
		GL30.glDeleteRenderbuffers(id);
		ExceptionUtil.glError();
	}

}
