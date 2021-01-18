package com.spaghettiengine.core;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4d;
import org.joml.Vector3d;
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

	protected Matrix4d renderMat = new Matrix4d();
	protected Matrix4d sceneMat = new Matrix4d();
	protected Vector3d vec3cache = new Vector3d();
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
		glDisable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		glEnable(GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				new int[] { 0, 1, 2, 2, 3, 0 });

		String vertSource = ResourceLoader.loadText("/internal/renderer.vs");
		String fragSource = ResourceLoader.loadText("/internal/renderer.fs");

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

	@Override
	protected void loopEvents(double delta) throws Throwable {
		if (window.shouldClose()) {
			terminate();
		}

		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		assetManager.lazyLoad();
		dispatcher.computeEvents();

		Camera camera = getCamera();
		if (camera != null) {

			// Draw camera to frame buffer

			FrameBuffer buffer = camera.getFrameBuffer();
			buffer.use();
			if (camera.getClearColor()) {
				glClear(GL11.GL_COLOR_BUFFER_BIT);
			}
			if (camera.getClearDepth()) {
				glClear(GL11.GL_DEPTH_BUFFER_BIT);
			}
			if (camera.getClearStencil()) {
				glClear(GL11.GL_STENCIL_BUFFER_BIT);
			}

			getLevel().forEachActualComponent((id, component) -> {

				// Reset matrix
				sceneMat.set(camera.getProjection());

				// Get world position
				component.getWorldPosition(vec3cache);
				sceneMat.translate(vec3cache);

				// Get world rotation
				component.getWorldRotation(vec3cache);
				sceneMat.rotateXYZ(vec3cache);

				// Get world scale
				component.getWorldScale(vec3cache);
				sceneMat.scale(vec3cache);

				component.render(sceneMat, delta);

			});

			buffer.stop();

			// Draw texture from frame buffer to screen

			{
				// Reset render matrix
				renderMat.identity();

				// Calculate the scale
				double scale = CMath.min(window.getWidth() / camera.getTargetRatio(), window.getHeight());

				// Scale the matrix accordingly dividing by window size
				renderMat.scale((scale * camera.getTargetRatio()) / window.getWidth(), -scale / window.getHeight(), 1);

				// Render textured quad
				defaultShader.use();
				defaultShader.setProjection(renderMat);
				camera.getFrameBuffer().getColorTexture().use(0);
				sceneRenderer.render();
			}

		}

		window.swap();

		fps++;

		if (System.currentTimeMillis() >= lastCheck + 1000) {
			Logger.info(source, fps + " FPS");
			fps = 0;
			lastCheck = System.currentTimeMillis();
		}
	}

	public void updateRenderMatrix() {

	}

}
