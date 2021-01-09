package com.spaghettiengine.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

public class TextureFrameBuffer extends FrameBuffer {

	protected Texture depth;

	public TextureFrameBuffer() {
	}

	public TextureFrameBuffer(int width, int height) {
		super(width, height);
	}
	
	@Override
	protected void load0() {
		super.load0();

		this.depth = new Texture((ByteBuffer) null, width, height) {

			@Override
			protected void setParameters(ByteBuffer buffer, int width, int height) {

				GL11.glBindTexture(GL11.GL_TEXTURE_2D, this.getId());

				GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL14.GL_DEPTH_COMPONENT32, width, height, 0,
						GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, buffer);

				int mode = GL11.GL_LINEAR;

				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, mode);
				GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mode);

			}

		};
		
		// Attach depth texture

		use();
		depth.use(0);
		GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D,
				depth.getId(), 0);
	}

	public Texture getDepth() {
		return depth;
	}

	protected void delete0() {
		super.delete0();
		
		depth.reset();
	}

	@Override
	protected void reset0() {
		depth = null;
	}

}
