package com.spaghettiengine.render;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import com.spaghettiengine.core.Game;

public class Texture extends RenderObject {

	public static Texture get(String name) {
		return Game.getGame().getAssetManager().texture(name);
	}
	
	public static Texture require(String name) {
		return Game.getGame().getAssetManager().requireTexture(name);
	}
	
	// Static method necessary for ausiliary constructor
	private static final ByteBuffer parseImage(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();

		// Prepare for copy
		int pixels_raw[] = new int[w * h * 4];
		pixels_raw = img.getRGB(0, 0, w, h, null, 0, w);
		ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);

		// Copy data into a byte buffer
		for (int pixel : pixels_raw) {
			pixels.put((byte) ((pixel >> 16) & 0xFF)); // Red
			pixels.put((byte) ((pixel >> 8) & 0xFF)); // Green
			pixels.put((byte) ((pixel) & 0xFF)); // Blue
			pixels.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
		}

		pixels.flip();

		return pixels;
	}

	protected int id;
	protected int width, height;
	protected ByteBuffer buffer;

	// This can be overridden to easily modify the behaviour of the construction
	// process
	protected void setParameters(ByteBuffer buffer, int width, int height) {
		
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, id);

		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

		GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
				buffer);
		
	}

	public Texture() {
	}
	
	public Texture(ByteBuffer buffer, int width, int height) {
		setData(buffer, width, height);
		load();
	}

	public Texture(int width, int height) {
		this((ByteBuffer) null, width, height);
	}

	public Texture(BufferedImage img) {
		this(parseImage(img), img.getWidth(), img.getHeight());
	}
	
	public void setData(ByteBuffer buffer, int width, int height) {
		if(valid()) {
			return;
		}
		
		this.buffer = buffer;
		this.width = width;
		this.height = height;
		
		setFilled(true);
	}

	@Override
	protected void load0() {
		// First generate a valid id for this texture
		id = GL11.glGenTextures();

		try {

			setParameters(buffer, width, height);

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
		if(!valid()) {
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

	@Override
	protected void reset0() {
		buffer = null;
		width = 0;
		height = 0;
		id = -1;
	}

}
