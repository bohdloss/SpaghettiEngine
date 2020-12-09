package com.spaghettiengine.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class RenderFrameBuffer extends FrameBuffer {

	protected int depth;

	public RenderFrameBuffer(int width, int height) {
		super(width, height);

		// Create a depth render buffer

		depth = GL30.glGenRenderbuffers();

		// Bind the render buffer

		GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depth);

		// Set the size of the render buffer

		GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL11.GL_DEPTH_COMPONENT, width, height);

		// Attach it to the FBO

		use();
		GL30.glFramebufferRenderbuffer(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depth);
		stop();

	}

	public int getRenderBuffer() {
		return depth;
	}

}
