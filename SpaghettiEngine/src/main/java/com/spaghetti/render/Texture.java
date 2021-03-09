package com.spaghetti.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.utils.Utils;

public class Texture extends Asset {

	public static Texture get(String name) {
		return Game.getGame().getAssetManager().texture(name);
	}

	public static Texture require(String name) {
		return Game.getGame().getAssetManager().requireTexture(name);
	}

	public static final int COLOR = GL30.GL_RGBA;
	public static final int DEPTH = GL30.GL_DEPTH_COMPONENT;
	public static final int STENCIL = GL30.GL_STENCIL_INDEX8;

	public static final int LINEAR = GL11.GL_LINEAR;
	public static final int NEAREST = GL11.GL_NEAREST;

	protected int id;
	protected int width, height;
	protected int type, mode;
	protected ByteBuffer buffer;

	// This can be overridden to easily modify the behaviour of the construction
	// process
	protected void setParameters(ByteBuffer buffer, int width, int height, int type, int mode) {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);

		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, type, width, height, 0, type, GL11.GL_UNSIGNED_BYTE, buffer);

	}

	public Texture() {
	}

	public Texture(ByteBuffer buffer, int width, int height) {
		this(buffer, width, height, COLOR);
	}

	public Texture(ByteBuffer buffer, int width, int height, int type) {
		this(buffer, width, height, type, LINEAR);
	}

	public Texture(ByteBuffer buffer, int width, int height, int type, int mode) {
		setData(buffer, width, height, type, mode);
		load();
	}

	public Texture(int width, int height) {
		this((ByteBuffer) null, width, height);
	}

	public Texture(BufferedImage img) {
		this(Utils.parseImage(img), img.getWidth(), img.getHeight());
	}

	@Override
	public void setData(Object... objects) {
		if (valid()) {
			return;
		}

		this.buffer = (ByteBuffer) objects[0];
		this.width = (int) objects[1];
		this.height = (int) objects[2];
		this.type = (int) objects[3];
		this.mode = (int) objects[4];
	}

	@Override
	public boolean isFilled() {
		return width > 0 && height > 0 && (type == COLOR || type == DEPTH || type == STENCIL)
				&& (mode == LINEAR || mode == NEAREST);
	}

	@Override
	protected void load0() {
		// Generate a valid id for this texture
		id = GL11.glGenTextures();

		try {

			setParameters(buffer, width, height, type, mode);

		} catch (Throwable t) {

			// Clean up before throwing any error
			delete();

			// Re-throw the exception
			throw t;

		} finally {
			// It's not needed, let's hope GC picks it up...
			buffer = null;
		}
	}

	public void use(int sampler) {
		if (!valid()) {
			return;
		}
		if (sampler >= 0 && sampler <= 31) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + sampler);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		}
	}

	@Override
	protected void delete0() {
		GL11.glDeleteTextures(id);
	}

	public int getId() {
		return id;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getType() {
		return type;
	}

	@Override
	protected void reset0() {
		buffer = null;
		width = 0;
		height = 0;
		id = -1;
	}

}
