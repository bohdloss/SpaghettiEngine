package com.spaghetti.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GLCapabilities;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.GameWindow;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public class Renderer extends CoreComponent {

	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected AssetManager assetManager;

	protected Matrix4d renderMatrix = new Matrix4d();
	protected Matrix4d sceneMatrix = new Matrix4d();
	protected Vector3f vec3cache = new Vector3f();
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;

	protected int fps;
	protected long lastCheck;

	@Override
	public void initialize0() throws Throwable {
		this.window = getGame().getWindow();
		this.assetManager = getGame().getAssetManager();

		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				new int[] { 0, 1, 2, 2, 3, 0 });

		defaultShader = ShaderProgram.require("rendererSP");

	}

	@Override
	protected void terminate0() throws Throwable {

	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		try {
			if (window.shouldClose()) {
				getGame().stopAsync();
			}

			Camera camera = getCamera();
			if (camera != null) {
				// Draw level to camera frame buffer
				camera.render(null, delta);

				// Draw texture from frame buffer to screen

				// Reset render matrix
				renderMatrix.identity();
				// Calculate the scale
				float scale = CMath.min(window.getWidth() / camera.getTargetRatio(), window.getHeight());
				// Scale the matrix accordingly dividing by window size
				renderMatrix.scale((scale * camera.getTargetRatio()) / window.getWidth(), -scale / window.getHeight(),
						1);
				// Render textured quad
				defaultShader.use();
				defaultShader.setProjection(renderMatrix);
				camera.getFrameBuffer().getColorTexture().use(0);
				GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);
				sceneRenderer.render();
			}

			window.swap();

			fps++;
		} catch (Throwable t) {
			Logger.error("Rendering generated an exception", t);
		}

		if (System.currentTimeMillis() >= lastCheck + 1000) {
			Logger.info(fps + " FPS");
			fps = 0;
			lastCheck = System.currentTimeMillis();
		}
	}

	@Override
	protected final CoreComponent provideSelf() {
		return getGame().getRenderer();
	}

}
