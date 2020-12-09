package com.spaghettiengine.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public abstract class FrameBuffer {

	private boolean deleted;
	protected int id;
	protected Texture color;
	protected int width, height;

	public FrameBuffer(int width, int height) {

		this.width = width;
		this.height = height;

		this.color = new Texture((ByteBuffer) null, width, height) {

			@Override
			protected void setParameters(ByteBuffer buffer, int width, int height) {

				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.getId());

				GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, width, height, 0, GL11.GL_RGBA,
						GL11.GL_UNSIGNED_BYTE, buffer);

				int mode = GL11.GL_LINEAR;

				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);

			}

		};

		// Create frame buffer

		id = GL30.glGenFramebuffers();

		// Attach texture

		use();
		GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
				color.getId(), 0);
		stop();

	}

	public void use() {
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
		GL11.glViewport(0, 0, width, height);
		GL11.glOrtho(-width / 2, -height / 2, width / 2, height / 2, -1, 1);
	}

	public static void stop() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public void delete() {
		if (!deleted) {
			GL30.glDeleteFramebuffers(id);

			deleted = true;
		}
	}

	@Override
	public void finalize() {
		delete();
	}

	public boolean isDeleted() {
		return deleted;
	}

	public final void checkValid() {
		use();
		int framebuffer = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		switch (framebuffer) {
		case GL30.GL_FRAMEBUFFER_COMPLETE:
			break;
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
			throw new RuntimeException(
					"FrameBuffer: " + id + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT exception");
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
			throw new RuntimeException(
					"FrameBuffer: " + id + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT exception");
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
			throw new RuntimeException(
					"FrameBuffer: " + id + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER exception");
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
			throw new RuntimeException(
					"FrameBuffer: " + id + ", has caused a GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER exception");
		default:
			throw new RuntimeException("Unexpected reply from glCheckFramebufferStatus: " + framebuffer);
		}
		stop();
	}

	public Texture getColor() {
		return color;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

}
