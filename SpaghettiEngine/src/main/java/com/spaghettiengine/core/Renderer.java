package com.spaghettiengine.core;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4d;
import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

import com.spaghettiengine.assets.AssetManager;
import com.spaghettiengine.components.Camera;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.*;

public class Renderer extends CoreComponent {

	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected FunctionDispatcher dispatcher;
	protected AssetManager assetManager;
	
	protected Matrix4d mat = new Matrix4d();
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;
	
	protected int fps;
	protected long lastCheck;

	public Renderer(Game source) {
		super(source);
	}

	@Override
	public void initialize0() throws Throwable {
		this.window = source.getWindow();
		this.dispatcher = source.getFunctionDispatcher();
		this.assetManager = source.getAssetManager();

		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		glDisable(GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, new int[] { 0, 1, 2, 2, 3, 0 });

		String vertSource = "#version 120\n" + "\n" + "attribute vec3 vertices;\n" + "attribute vec2 textures;\n" + "attribute vec3 normals;\n"
				+ "varying vec2 tex_coords;\n" + "\n" + "uniform mat4 projection;\n" + "\n" + "void main() {\n"
				+ "	tex_coords = textures;\n" + "	gl_Position = projection * vec4(vertices, 1.0);\n" + "}";

		String fragSource = "# version 120\n" +
				"\n" +
				"uniform sampler2D sampler;\n" +
				"\n" +
				"varying vec2 tex_coords;\n" +
				"\n" +
				"void main() {\n" +
				"	vec4 colorVec = texture2D(sampler, tex_coords);\n" +
				"	float x = tex_coords.x;\n" +
				"	float y = tex_coords.y;\n" +
				"	if(x < 0.05 || x > 0.95 || y < 0.05 || y > 0.95) {\n" +
				"		colorVec.x = 1;\n" +
				"		colorVec.w = 1;\n" +
				"	}\n" +
				"	gl_FragColor = colorVec;\n" +
				"}";
		
		Shader vertex = new Shader(vertSource, Shader.VERTEX_SHADER);
		Shader fragment = new Shader(fragSource, Shader.FRAGMENT_SHADER);

		defaultShader = new ShaderProgram(vertex, fragment);

		vertex.delete();
		fragment.delete();
	}

	@Override
	protected void terminate0() throws Throwable {
		assetManager.destroy();
	}
	
	Matrix4d a = new Matrix4d();
	
	@Override
	protected void loopEvents() throws Throwable {
		if (window.shouldClose()) {
			terminate();
		}

		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		assetManager.lazyLoad();
		dispatcher.computeEvents(1);

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
			//sceneRenderer.render();

		}

		window.swap();

		fps++;

		if (System.currentTimeMillis() >= lastCheck + 1000) {
			Logger.info(source, fps + " FPS");
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
