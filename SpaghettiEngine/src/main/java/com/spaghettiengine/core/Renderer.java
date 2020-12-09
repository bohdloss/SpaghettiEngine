package com.spaghettiengine.core;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4d;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.*;

public class Renderer extends CoreComponent {

	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected FunctionDispatcher dispatcher;
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;
	protected Texture texture;
	protected Matrix4d mat = new Matrix4d();

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

		sceneRenderer = new Model(new float[] { -0.5f, 0.5f, 0f, 0.5f, 0.5f, 0f, 0.5f, -0.5f, 0f, -0.5f, -0.5f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new int[] { 0, 1, 2, 2, 3, 0 });

		String vertSource = "#version 120\n" + "\n" + "attribute vec3 vertices;\n" + "attribute vec2 textures;\n" + "\n"
				+ "varying vec2 tex_coords;\n" + "\n" + "uniform mat4 projection;\n" + "\n" + "void main() {\n"
				+ "	tex_coords = textures;\n" + "	gl_Position = projection * vec4(vertices, 1.0);\n" + "}";

		String fragSource = "# version 120\n" + "\n" + "uniform sampler2D sampler;\n" + "\n"
				+ "varying vec2 tex_coords;\n" + "\n" + "void main() {\n"
				+ "	gl_FragColor = texture2D(sampler, tex_coords);\n" + "}";

		Shader vertex = new Shader(vertSource, Shader.VERTEX_SHADER);
		Shader fragment = new Shader(fragSource, Shader.FRAGMENT_SHADER);

		defaultShader = new ShaderProgram(vertex, fragment);

		vertex.delete();
		fragment.delete();

		try {
			texture = new Texture(ResourceLoader.loadImage("/data/holesom2.png"));
		} catch (Throwable e) {
			e.printStackTrace();
		}

		mat.scale(2, 2, 1);

		super.init();
	}

	@Override
	protected void loopEvents() {
		if (window.shouldClose()) {
			terminate();
		}

		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		dispatcher.computeEvents();

		Camera camera = getCamera();
		if (camera != null) {

			// Draw camera to frame buffer

			camera.draw();

			// Draw texture from frame buffer to screen

			glViewport(0, 0, window.width, window.height);
			glOrtho(-window.width / 2, -window.height / 2, window.height / 2, window.height / 2, -1, 1);
			defaultShader.use();
			defaultShader.setProjection(mat);
			camera.getFrameBuffer().getColor().use(0);
			sceneRenderer.render();

		}

		window.swap();

		fps++;

		if (System.currentTimeMillis() >= lastCheck + 1000) {
			System.out.println(fps + " FPS");
			fps = 0;
			lastCheck = System.currentTimeMillis();
		}
	}

	// Getters and setters

	protected Level getLevel() {
		return source.activeLevel;
	}

	protected Camera getCamera() {
		if (getLevel() == null) {
			return null;
		}
		return getLevel().getActiveCamera();
	}

}
