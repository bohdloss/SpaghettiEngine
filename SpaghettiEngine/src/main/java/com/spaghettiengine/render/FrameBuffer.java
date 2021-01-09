package com.spaghettiengine.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.spaghettiengine.utils.Logger;

public abstract class FrameBuffer extends RenderObject {

	protected int id;
	protected Texture color;
	protected int width, height;

	public FrameBuffer(int width, int height) {
		setData(width, height);
		load();
	}

	public FrameBuffer() {
	}

	public void setData(int width, int height) {
		if(valid()) {
			return;
		}
		
		this.width = width;
		this.height = height;
		
		setFilled(true);
	}

	@Override
	protected void load0() {
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
		if (!valid()) {
			return;
		}

/*		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
		GL11.glViewport(0, 0, width, height);
		GL11.glOrtho(-width / 2, width / 2, -height / 2, height / 2, -1, 1);*/
	}

	public static void stop() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	@Override
	protected void delete0() {
		GL30.glDeleteFramebuffers(id);
	}

	public final void checkValid() {
		if (!valid()) {
			Logger.warning("Checking FrameBuffer aborted (not initialized yet)");
			throw new RuntimeException("FrameBuffer has not been initialized");
		}

		use();
		int framebuffer = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
		String message;
		boolean statusOK = false;
		switch (framebuffer) {
		case GL30.GL_FRAMEBUFFER_COMPLETE:
			message = "Checked FrameBuffer " + id + ", status OK";
			statusOK = true;
			break;
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT)";
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)";
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)";
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER)";
		default:
			message = "FrameBuffer: " + id + " in invalid state!";
		}

		stop();

		if (statusOK) {
			Logger.info(message);
		} else {
			Logger.warning(message);
			throw new IllegalStateException(message);
		}
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
