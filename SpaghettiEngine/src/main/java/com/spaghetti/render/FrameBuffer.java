package com.spaghetti.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.core.GameWindow;
import com.spaghetti.utils.Logger;

public class FrameBuffer extends Asset {

	protected int id;
	protected Asset color, depth, stencil;
	protected int width, height;

	public FrameBuffer(int width, int height) {
		setData(new Texture((ByteBuffer) null, width, height, Texture.COLOR),
				new Texture((ByteBuffer) null, width, height, Texture.DEPTH), null);
		load();
	}

	public FrameBuffer() {
	}

	@Override
	public void setData(Object... objects) {
		if (valid()) {
			return;
		}

		this.color = (Asset) objects[0];
		this.depth = (Asset) objects[1];
		this.stencil = (Asset) objects[2];

		if (Texture.class.isAssignableFrom(color.getClass())) {
			Texture cast = (Texture) color;
			this.width = cast.getWidth();
			this.height = cast.getHeight();
		} else if (RenderBuffer.class.isAssignableFrom(color.getClass())) {
			RenderBuffer cast = (RenderBuffer) color;
			this.width = cast.getWidth();
			this.height = cast.getHeight();
		}

	}

	@Override
	public boolean isFilled() {
		return (color != null || depth != null || stencil != null) && height > 0 && width > 0;
	}

	@Override
	protected void load0() {

		// Create frame buffer
		id = GL30.glGenFramebuffers();

		// Attach color
		attachColor(color);

		// Attach depth
		attachDepth(depth);

		// Attach stencil
		attachStencil(stencil);

		// Check for validity
		checkValid();

	}

	private void wrongFormat() {
		Logger.warning("Trying to attach object of invalid format to FrameBuffer " + id);
	}

	protected void attachColor(Asset object) {
		if (object == null) {
			return;
		}

		internal_use();
		if (Texture.class.isAssignableFrom(object.getClass())) {
			Texture cast = (Texture) object;
			if (cast.getType() != Texture.COLOR) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
					cast.getId(), 0);

		} else if (RenderBuffer.class.isAssignableFrom(object.getClass())) {

			RenderBuffer cast = (RenderBuffer) object;
			if (cast.getType() != RenderBuffer.COLOR) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_RENDERBUFFER,
					cast.getId());

		}
		internal_stop();

	}

	protected void attachDepth(Asset object) {
		if (object == null) {
			return;
		}

		internal_use();
		if (Texture.class.isAssignableFrom(object.getClass())) {

			Texture cast = (Texture) object;
			if (cast.getType() != Texture.DEPTH) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D,
					cast.getId(), 0);

		} else if (RenderBuffer.class.isAssignableFrom(object.getClass())) {

			RenderBuffer cast = (RenderBuffer) object;
			if (cast.getType() != RenderBuffer.DEPTH) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER,
					cast.getId());

		}
		internal_stop();

	}

	protected void attachStencil(Asset object) {
		if (object == null) {
			return;
		}

		internal_use();
		if (Texture.class.isAssignableFrom(object.getClass())) {

			Texture cast = (Texture) object;
			if (cast.getType() != Texture.STENCIL) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL11.GL_TEXTURE_2D,
					cast.getId(), 0);

		} else if (RenderBuffer.class.isAssignableFrom(object.getClass())) {

			RenderBuffer cast = (RenderBuffer) object;
			if (cast.getType() != RenderBuffer.STENCIL) {
				wrongFormat();
				return;
			}
			GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER,
					cast.getId());
		}
		internal_stop();

	}

	public void use() {
		if (!valid()) {
			return;
		}

		GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
		internal_use();
		GL11.glViewport(0, 0, width, height);
		GL11.glOrtho(-width / 2, width / 2, -height / 2, height / 2, -1, 1);
	}

	public void stop() {
		GameWindow window = Game.getGame().getWindow();
		internal_stop();
		GL11.glViewport(0, 0, window.getWidth(), window.getHeight());
		GL11.glOrtho(-window.getWidth() / 2, window.getWidth() / 2, -window.getHeight() / 2, window.getHeight() / 2, -1,
				1);
	}

	@Override
	protected void delete0() {
		GL30.glDeleteFramebuffers(id);
	}

	protected void internal_use() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, id);
	}

	protected void internal_stop() {
		GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
	}

	public final void checkValid() {
		internal_use();
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
			break;
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT)";
			break;
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER)";
			break;
		case GL30.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
			message = "FrameBuffer " + id + " is incomplete! (GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER)";
			break;
		default:
			message = "FrameBuffer: " + id + " in invalid state! (" + framebuffer + "). "
					+ "If you are seeing this message the error is most likely due to the configuration of attachments";
		}

		internal_stop();

		if (statusOK) {
			Logger.info(message);
		} else {
			Logger.warning(message);
			throw new IllegalStateException(message);
		}
	}

	public int getId() {
		return id;
	}

	public Texture getColorTexture() {
		return (Texture) color;
	}

	public RenderBuffer getColorBuffer() {
		return (RenderBuffer) color;
	}

	public Texture getDepthTexture() {
		return (Texture) depth;
	}

	public RenderBuffer getDepthBuffer() {
		return (RenderBuffer) depth;
	}

	public Texture getStencilTexture() {
		return (Texture) stencil;
	}

	public RenderBuffer getStencilBuffer() {
		return (RenderBuffer) stencil;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	@Override
	protected void reset0() {

	}

}
