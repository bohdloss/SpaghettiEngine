package com.spaghettiengine.core;

import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.*;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

import com.spaghettiengine.utils.FunctionDispatcher;

public class Renderer extends CoreComponent {

	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected FunctionDispatcher dispatcher;

	protected int fps;
	protected long lastCheck;
	
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
		glEnable(GL_DEPTH_TEST);
		glEnable(GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		super.init();
	}

	@Override
	protected void loopEvents() {
		if (window.shouldClose()) {
			terminate();
		}
		dispatcher.computeEvents();
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

		if(source.activeLevel != null) {
			source.activeLevel.render();
		}
		
		window.swap();
		
		fps++;
		
		if(System.currentTimeMillis() >= lastCheck + 1000) {
			System.out.println(fps + " FPS");
			fps = 0;
			lastCheck = System.currentTimeMillis();
		}
	}

	// Getters and setters

}
