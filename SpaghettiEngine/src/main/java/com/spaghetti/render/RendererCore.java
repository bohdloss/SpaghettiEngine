package com.spaghetti.render;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix4d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageCallbackI;
import org.lwjgl.system.MemoryUtil;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.GameWindow;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public class RendererCore extends CoreComponent {

	// Internal data
	protected GameWindow window;
	protected GLCapabilities glCapabilities;
	protected ALCapabilities alCapabilities;
	protected long alOutputDevice;
	protected long alContext;
	protected AssetManager assetManager;

	// Options
	protected boolean openal = true;

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

	public RendererCore() {
		window = new GameWindow();
	}

	@Override
	public void initialize0() throws Throwable {
		this.window.winInit(getGame());
		this.assetManager = getGame().getAssetManager();

		// Initializes OpenGL
		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		GL43.glDebugMessageCallback(new GLDebugMessageCallbackI() {

			@Override
			public void invoke(int source, int type, int id, int severity, int length, long message, long userParam) {
				GLException error = new GLException(source, type, id, severity, MemoryUtil.memUTF8(message, length));
				
				// Remove useless lines from stacktrace
				final int useless_lines = 2;
				StackTraceElement[] stack = error.getStackTrace();
				StackTraceElement[] new_stack = new StackTraceElement[stack.length - useless_lines];
				System.arraycopy(stack, useless_lines, new_stack, 0, new_stack.length);
				error.setStackTrace(new_stack);
				
				throw error;
			}
			
		}, 0);
		
		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_LIGHTING);
		GLFW.glfwSwapInterval(0);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				new int[] { 0, 1, 2, 2, 3, 0 });

		defaultShader = ShaderProgram.require("rendererSP");

		if (openal) {
			// Initializes OpenAL output
			alOutputDevice = ALC10.alcOpenDevice((ByteBuffer) null);
			ALCCapabilities alcCapabilities = ALC.createCapabilities(alOutputDevice);

			alContext = ALC10.alcCreateContext(alOutputDevice, (IntBuffer) null);
			ALC10.alcMakeContextCurrent(alContext);
			ALC10.alcProcessContext(alContext);
			alCapabilities = AL.createCapabilities(alcCapabilities);
		}
	}

	@Override
	protected void postInitialize() throws Throwable {
//		for(String device : MicrophoneInputStream.getCaptureDevices()) {
//			System.out.println(device);
//		}
//		StreamProvider provider = (StreamProvider) () -> new MicrophoneInputStream();
//		
//		StreamingSound sound = new StreamingSound();
//		sound.setData(MicrophoneInputStream.DEFAULT_FORMAT, MicrophoneInputStream.DEFAULT_FREQUENCY,
//				MicrophoneInputStream.BPS, ByteOrder.nativeOrder(), provider, 2, 1000);
//		sound.load();
//		
//		SoundSource source = new SoundSource(sound);
//		source.play();
//		getGame().getActiveLevel().addObject(source);
	}

	@Override
	protected void terminate0() throws Throwable {
		if (openal) {
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
			GL43.glGetError();
			GL43.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			if (window.shouldClose()) {
				getGame().stopAsync();
			}

			Camera camera = getCamera();
			if (camera != null) {
				if (openal) {
					// Update listener position and velocity
					Vector3f camerapos = new Vector3f();
					camera.getWorldPosition(camerapos);
					Vector3f cameravel = new Vector3f();
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

	// Getters and setters

	public GameWindow getWindow() {
		return window;
	}

	public boolean isOpenALEnabled() {
		return openal;
	}

	public void setOpenALEnabled(boolean value) {
		this.openal = value;
	}

}