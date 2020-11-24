package com.spaghettiengine.core;

import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

public class Renderer extends CoreComponent {

	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected FunctionDispatcher dispatcher;

	public Renderer(Game source) {
		super(source);
	}

	@Override
	public void init() {
		this.window = source.getWindow();
		this.dispatcher = source.getFunctionDispatcher();
		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glDisable(GL_CULL_FACE);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		super.init();
	}

	@Override
	protected void loopEvents() {
		if (window.shouldClose()) {
			terminate();
		}
		dispatcher.computeEvents();
		glClear(GL_COLOR_BUFFER_BIT);

		window.swap();
	}

	// Getters and setters

}
