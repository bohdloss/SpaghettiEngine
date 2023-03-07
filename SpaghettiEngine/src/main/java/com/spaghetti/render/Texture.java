package com.spaghetti.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import com.spaghetti.audio.Sound;
import com.spaghetti.utils.ExceptionUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.utils.ImageUtils;

public class Texture extends Asset {

	public static Texture get(String name) {
		return Game.getInstance().getAssetManager().getAndLazyLoadAsset(name);
	}

	public static Texture require(String name) {
		return Game.getInstance().getAssetManager().getAndInstantlyLoadAsset(name);
	}

	public static Texture getDefault() {
		return Game.getInstance().getAssetManager().getDefaultAsset("Texture");
	}

	public static final int COLOR = GL30.GL_RGBA;
	public static final int DEPTH = GL30.GL_DEPTH_COMPONENT;

	public static final int LINEAR = GL11.GL_LINEAR;
	public static final int NEAREST = GL11.GL_NEAREST;

	protected int id;
	protected int width, height;
	protected int type, mode;
	protected ByteBuffer buffer;

	// This can be overridden to easily modify the behavior of the construction
	// process
	protected void setParameters(ByteBuffer buffer, int width, int height, int type, int mode) {
		// Bind
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		ExceptionUtil.glError();

		// Set mag and min filters
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
		ExceptionUtil.glError();
		GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);
		ExceptionUtil.glError();

		// Set texture data
		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, type, width, height, 0, type, GL11.GL_UNSIGNED_BYTE, buffer);
		ExceptionUtil.glError();

		// Free buffer
		MemoryUtil.memFree(buffer);
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
		setData(new Object[] {buffer, width, height, type, mode});
		load();
	}

	public Texture(int width, int height) {
		this((ByteBuffer) null, width, height);
	}

	public Texture(BufferedImage img) {
		this(ImageUtils.parseImage(img, BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4)),
				img.getWidth(), img.getHeight());
	}

	@Override
	public void setData(Object[] objects) {
		if (isValid()) {
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
		return width > 0 && height > 0 && (type == COLOR || type == DEPTH) && (mode == LINEAR || mode == NEAREST);
	}

	@Override
	protected void load0() {
		// Generate a valid id for this texture
		id = GL11.glGenTextures();
		ExceptionUtil.glError();

		try {
			setParameters(buffer, width, height, type, mode);
		} catch (Throwable t) {
			// Clean up before throwing any error
			unload();
			// Re-throw the exception
			throw t;
		} finally {
			// It's not needed, let's hope GC picks it up...
			buffer = null;
		}
	}

	public void use(int sampler) {
		if (!isValid()) {
			Texture base = getDefault();
			if(this != base) {
				base.use(sampler);
			}
			return;
		}
		if (sampler >= 0 && sampler <= 31) {
			GL13.glActiveTexture(GL13.GL_TEXTURE0 + sampler);
			GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);
		}
	}

	@Override
	protected void unload0() {
		GL11.glDeleteTextures(id);
		ExceptionUtil.glError();
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

}
