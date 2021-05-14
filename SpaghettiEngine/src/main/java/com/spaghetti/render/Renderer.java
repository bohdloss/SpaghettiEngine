package com.spaghetti.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GLCapabilities;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.GameWindow;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public class Renderer extends CoreComponent {

	// Internal data
	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected ALCapabilities alCapabilities;
	protected long alOutputDevice;
	protected long alContext;
	protected long alInputDevice;
	protected AssetManager assetManager;

	// Options
	protected boolean openal_output = true;
	protected boolean openal_input = false;
	protected int openal_input_samplerate = 8000;
	protected int openal_input_format = MONO_16;
	protected int openal_input_buffersize = 800;

	public static final int MONO_8 = AL11.AL_FORMAT_MONO8;
	public static final int MONO_16 = AL11.AL_FORMAT_MONO16;
	public static final int STEREO_8 = AL11.AL_FORMAT_STEREO8;
	public static final int STEREO_16 = AL11.AL_FORMAT_STEREO16;

	// Cache
	protected Matrix4d renderMatrix = new Matrix4d();
	protected Matrix4d sceneMatrix = new Matrix4d();
	protected Vector3f camerapos = new Vector3f();
	protected Vector3f camerapos_old = new Vector3f();
	protected Vector3f cameravel = new Vector3f();
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;

	// FPS counter
	protected int fps;
	protected long lastCheck;

	public Renderer() {
		window = new GameWindow();
	}

	@Override
	public void initialize0() throws Throwable {
		this.window.winInit(getGame());
		this.assetManager = getGame().getAssetManager();

		// Initializes OpenGL
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

		if (openal_output) {
			// Initializes OpenAL output
			alOutputDevice = ALC10.alcOpenDevice((ByteBuffer) null);
			ALCCapabilities alcCapabilities = ALC.createCapabilities(alOutputDevice);

			alContext = ALC10.alcCreateContext(alOutputDevice, (IntBuffer) null);
			ALC10.alcMakeContextCurrent(alContext);
			ALC10.alcProcessContext(alContext);
			alCapabilities = AL.createCapabilities(alcCapabilities);
		}
		if (openal_input) {
			// Initializes OpenAL input
			alInputDevice = ALC11.alcCaptureOpenDevice((ByteBuffer) null, openal_input_samplerate, openal_input_format,
					openal_input_buffersize);
		}
	}

	@Override
	protected void terminate0() throws Throwable {
		if (openal_input) {
			// Terminates OpenAL input
			ALC11.alcCaptureStop(alInputDevice);
			ALC11.alcCaptureCloseDevice(alInputDevice);
		}
		if (openal_output) {
			// Terminates OpenAL
			ALC10.alcMakeContextCurrent(0);
			ALC10.alcDestroyContext(alContext);
			ALC10.alcCloseDevice(alOutputDevice);
		}

		// Terminates OpenGL
		boolean async = window.isAsync();
		window.setAsync(true);
		window.destroy();
		window.setAsync(async);
	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		try {
			if (window.shouldClose()) {
				getGame().stopAsync();
			}
			Camera camera = getCamera();
			if (camera != null) {
				if (openal_output) {
					// Set position and velocity of OpenAL listener to match the active camera
					camera.getWorldPosition(camerapos);
					// This computes the velocity of the camera since last frame
					camerapos.sub(camerapos_old, cameravel);
					if (delta == 0) {
						cameravel.set(0);
					} else {
						cameravel.div(delta / 1000);
					}
					AL10.alListener3f(AL10.AL_POSITION, camerapos.x, camerapos.y, camerapos.z);
					AL10.alListener3f(AL10.AL_VELOCITY, cameravel.x, cameravel.y, cameravel.z);
					camerapos_old.set(camerapos);
				}

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
			} else {

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

	// Getters and setters

	public GameWindow getWindow() {
		return window;
	}

	public boolean isOpenALOutputEnabled() {
		return openal_output;
	}

	public void setOpenALOutputEnabled(boolean value) {
		this.openal_output = value;
	}

	public boolean isOpenALInputEnabled() {
		return openal_input;
	}

	public void setOpenALInputEnabled(boolean value) {
		this.openal_input = value;
	}

	public int getOpenalInputSamplerate() {
		return openal_input_samplerate;
	}

	public void setOpenalInputSamplerate(int openal_input_samplerate) {
		this.openal_input_samplerate = openal_input_samplerate;
	}

	public int getOpenalInputFormat() {
		return openal_input_format;
	}

	public void setOpenalInputFormat(int openal_input_format) {
		this.openal_input_format = openal_input_format;
	}

	public int getOpenalInputBuffersize() {
		return openal_input_buffersize;
	}

	public void setOpenalInputBuffersize(int openal_input_buffersize) {
		this.openal_input_buffersize = openal_input_buffersize;
	}

}