package com.spaghetti.render;

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
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryUtil;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.GameWindow;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.CMath;
import com.spaghetti.utils.GLException;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Transform;

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

	// 2.1 MB render cache
	protected Transform[] renderCache;
	// 256 KB render cache allocation table
	protected boolean[] renderCache_alloc;
	// Cache locks
	protected boolean renderCache_frameflag;
	protected Object renderCache_framelock = new Object();

	// FPS counter
	protected int fps;
	protected long lastCheck;

	public RendererCore() {
		window = new GameWindow();
		// Init render cache
		renderCache = new Transform[Short.MAX_VALUE];
		for(int i = 0; i < renderCache.length; i++) {
			renderCache[i] = new Transform();
		}
		renderCache_alloc = new boolean[Short.MAX_VALUE];
	}

	@Override
	public void initialize0() throws Throwable {

		// Init window and obtain asset manager
		this.window.winInit(getGame());
		this.assetManager = getGame().getAssetManager();

		// Initializes OpenGL
		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		GL43.glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
			String message_str = MemoryUtil.memUTF8(message, length);
			if(severity <= GL43.GL_DEBUG_SEVERITY_NOTIFICATION) {
				Logger.info(getGame(), "[NOTIFICATION] OpenGL: ");
				Logger.info(getGame(), "[NOTIFICATION] " + message_str);
			} else if(severity <= GL43.GL_DEBUG_SEVERITY_LOW) {
				Logger.warning(getGame(), "[LOW] OpenGL: ");
				Logger.warning(getGame(), "[LOW] " + message_str);
			} else {
				GLException error = new GLException(source, type, id, severity, message_str);

				// Remove useless lines from stacktrace
				final int useless_lines = 2;
				StackTraceElement[] stack = error.getStackTrace();
				StackTraceElement[] new_stack = new StackTraceElement[stack.length - useless_lines];
				System.arraycopy(stack, useless_lines, new_stack, 0, new_stack.length);
				error.setStackTrace(new_stack);

				if(severity == GL43.GL_DEBUG_SEVERITY_MEDIUM) {
					Logger.error(getGame(), "[MEDIUM] OpenGL: ");
					Logger.error(getGame(), "[MEDIUM] " + message_str, error);
				} else if(severity == GL43.GL_DEBUG_SEVERITY_HIGH) {
					Logger.error(getGame(), "[HIGH] OpenGL: ");
					Logger.error(getGame(), "[HIGH] " + message_str, error);
				}

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

		// Free render cache
		for(int i = 0; i < renderCache_alloc.length; i++) {
			if(renderCache_alloc[i]) {
				Logger.info("Block " + i + " was not freed");
			}
		}
		renderCache = null;
		renderCache_alloc = null;
	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		try {
			GL11.glGetError();
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			if (window.shouldClose()) {
				getGame().stopAsync();
			}

			Camera camera = getCamera();
			if (camera != null) {
				Transform transform;
				int camera_index = camera.getRenderCacheIndex();
				if(camera_index == -1) {
					transform = new Transform();
					camera.getWorldPosition(transform.position);
					camera.getWorldRotation(transform.rotation);
					camera.getWorldScale(transform.scale);
				} else {
					transform = getCache(camera_index);
				}

				if (openal) {
					// Update listener position and velocity
					Vector3f camerapos = transform.position;
					Vector3f cameravel = new Vector3f();
					camerapos.sub(camerapos_old, cameravel);

					AL10.alListener3f(AL10.AL_POSITION, camerapos.x, camerapos.y, camerapos.z);
					if (delta != 0) {
						cameravel.div(delta / 1000);
						AL10.alListener3f(AL10.AL_VELOCITY, cameravel.x, cameravel.y, cameravel.z);
					}
					camerapos_old.set(camerapos);
				}

				// Draw level to camera frame buffer
				synchronized(renderCache_framelock) {
					renderCache_frameflag = true;
					camera.render(null, delta, transform);
				}
				renderCache_frameflag = false;

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

	public int allocCache() {
		if(renderCache == null) {
			return -1;
		}

		for(int i = 0; i < renderCache_alloc.length; i++) {
			if(!renderCache_alloc[i]) {
				renderCache_alloc[i] = true;

				Logger.info("Allocted render cache at index " + i);

				return i;
			}
		}
		return -1;
	}

	public void deallocCache(int index) {
		if((renderCache == null) || index < 0 || index > renderCache_alloc.length) {
			return;
		}

		Logger.info("Deallocated render cache at index " + index);

		renderCache_alloc[index] = false;
	}

	public Transform getCache(int index) {
		return renderCache[index];
	}

	public boolean isFrameFlag() {
		return renderCache_frameflag;
	}

	public Object getFrameLock() {
		return renderCache_framelock;
	}

}