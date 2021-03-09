package com.spaghetti.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Matrix4d;
import org.joml.Vector3d;
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
	protected Vector3d vec3cache = new Vector3d();
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
		glEnable(GL_TEXTURE_2D);
		glEnable(GL_BLEND);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		glEnable(GL_CULL_FACE);
		glEnable(GL_DEPTH_TEST);
		glDepthFunc(GL_LEQUAL);
		glDisable(GL_LIGHTING);
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
	protected void loopEvents(double delta) throws Throwable {
		if (window.shouldClose()) {
			getGame().stopAsync();
		}
		glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

		Camera camera = getCamera();
		if (camera != null) {

			// Draw camera to frame w_buffer

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

			getLevel().forEachActualObject((id, component) -> {

				// Reset matrix
				sceneMatrix.set(camera.getProjection());

				// Get world position
				component.getWorldPosition(vec3cache);
				sceneMatrix.translate(vec3cache);

				// Get world rotation
				component.getWorldRotation(vec3cache);
				sceneMatrix.rotateXYZ(vec3cache);

				// Get world scale
				component.getWorldScale(vec3cache);
				sceneMatrix.scale(vec3cache.x, vec3cache.y, 1);

				component.render(sceneMatrix, delta);

			});

			buffer.stop();

			// Draw texture from frame w_buffer to screen

			// Reset render matrix
			renderMatrix.identity();

			// Calculate the scale
			double scale = CMath.min(window.getWidth() / camera.getTargetRatio(), window.getHeight());

			// Scale the matrix accordingly dividing by window size
			renderMatrix.scale((scale * camera.getTargetRatio()) / window.getWidth(), -scale / window.getHeight(), 1);

			// Render textured quad
			defaultShader.use();
			defaultShader.setProjection(renderMatrix);
			camera.getFrameBuffer().getColorTexture().use(0);
			sceneRenderer.render();

		}

		window.swap();

		fps++;

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
