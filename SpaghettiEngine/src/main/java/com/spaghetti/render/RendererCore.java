package com.spaghetti.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.spaghetti.assets.loaders.*;
import com.spaghetti.core.events.ExitRequestedEvent;
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
import com.spaghetti.exceptions.GLException;
import com.spaghetti.utils.MathUtil;
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
	protected Camera lastCamera;
	protected Matrix4d renderMatrix = new Matrix4d();
	protected Matrix4d sceneMatrix = new Matrix4d();
	protected Vector3f camerapos = new Vector3f();
	protected Vector3f camerapos_old = new Vector3f();
	protected Vector3f cameravel = new Vector3f();
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;

	// 2.1 MB transform cache
	protected Transform[] transformCache;
	// 2.1 MB velocity cache
	protected Transform[] velocityCache;
	// 256 KB render cache allocation table
	protected boolean[] renderCache_alloc;
	// Last cache update
	protected long lastCacheUpdate;
	protected long currentTime;
	// Cache locks
	protected boolean renderCache_frameflag;
	protected Object renderCache_framelock = new Object();

	// FPS counter
	protected int fps;
	protected long lastCheck;

	public RendererCore() {
		window = new GameWindow();
		// Init render cache
		transformCache = new Transform[Short.MAX_VALUE];
		for(int i = 0; i < transformCache.length; i++) {
			transformCache[i] = new Transform();
		}
		velocityCache = new Transform[Short.MAX_VALUE];
		for(int i = 0; i < velocityCache.length; i++) {
			velocityCache[i] = new Transform();
			velocityCache[i].scale.set(0);
		}
		renderCache_alloc = new boolean[Short.MAX_VALUE];
	}

	@Override
	public void initialize0() throws Throwable {

		// Init window and obtain asset manager
		window.winInit(getGame());
		assetManager = getGame().getAssetManager();

		// Initializes OpenGL
		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		GL43.glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
			String message_str = MemoryUtil.memUTF8(message, length);
			if(severity <= GL43.GL_DEBUG_SEVERITY_NOTIFICATION) {
				Logger.debug(getGame(), "[NOTIFICATION] OpenGL: ");
				Logger.debug(getGame(), "[NOTIFICATION] " + message_str);
			} else if(severity <= GL43.GL_DEBUG_SEVERITY_LOW) {
				Logger.debug(getGame(), "[LOW] OpenGL: ");
				Logger.debug(getGame(), "[LOW] " + message_str);
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

		// Register asset loaders
		assetManager.registerAssetLoader("Model", new ModelLoader());
		assetManager.registerAssetLoader("VertexShader", new VertexShaderLoader());
		assetManager.registerAssetLoader("FragmentShader", new FragmentShaderLoader());
		assetManager.registerAssetLoader("ShaderProgram", new ShaderProgramLoader());
		assetManager.registerAssetLoader("Texture", new TextureLoader());
		assetManager.registerAssetLoader("Material", new MaterialLoader());

		if (openal) {
			// Initializes OpenAL output
			alOutputDevice = ALC10.alcOpenDevice((ByteBuffer) null);
			ALCCapabilities alcCapabilities = ALC.createCapabilities(alOutputDevice);

			alContext = ALC10.alcCreateContext(alOutputDevice, (IntBuffer) null);
			ALC10.alcMakeContextCurrent(alContext);
			ALC10.alcProcessContext(alContext);
			alCapabilities = AL.createCapabilities(alcCapabilities);

			// Register audio assets

			assetManager.registerAssetLoader("Music", new MusicLoader());
			assetManager.registerAssetLoader("Sound", new SoundLoader());
		}

		// Load asset sheets
		assetManager.loadAssetSheet(getGame().getEngineOption("assets.internalSheet"));
		assetManager.loadAssetSheet(getGame().getEngineOption("assetsheet"));

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_LIGHTING);
		GLFW.glfwSwapInterval(1);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				new int[] { 0, 1, 2, 2, 3, 0 });

		defaultShader = ShaderProgram.require("rendererSP");
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
		transformCache = null;
		velocityCache = null;
		renderCache_alloc = null;
	}

	@Override
	protected void loopEvents(float delta) throws Throwable {
		currentTime += (long) (delta * 1000f);
		try {
			GL11.glGetError();
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			if (window.shouldClose()) {
				ExitRequestedEvent event = new ExitRequestedEvent();
				getGame().getEventDispatcher().raiseEvent(event);
				if(event.isCancelled()) {
					window.setShouldClose(false);
				} else {
					getGame().stopAsync();
				}
			}

			Camera camera = getCamera();
			if (camera != null) {
				synchronized (renderCache_framelock) {
					Transform transform = new Transform();
					int camera_index = camera.getRenderCacheIndex();
					if (camera_index == -1) {
						camera.getWorldPosition(transform.position);
						camera.getWorldRotation(transform.rotation);
						camera.getWorldScale(transform.scale);
					} else {
						//transform = getTransformCache(camera_index);
						Transform trans = getGame().getRenderer().getTransformCache(camera_index);
						Transform vel = getGame().getRenderer().getVelocityCache(camera_index);
						float velDelta = getGame().getRenderer().getCacheUpdateDelta();

						vel.position.mul(velDelta, transform.position);
						transform.position.add(trans.position);

						vel.rotation.mul(velDelta, transform.rotation);
						transform.rotation.add(trans.rotation);

						transform.scale.set(0);
						vel.scale.mul(velDelta, transform.scale);
						transform.scale.add(trans.scale);
					}

					if (false) {
						// Update listener position and velocity
						if (camera != lastCamera) {
							lastCamera = camera;
							camerapos_old.set(transform.position);
						}
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
					renderCache_frameflag = true;
					camera.render(null, delta, transform);
					renderCache_frameflag = false;
				}
				// Draw texture from frame buffer to screen

				// Reset render matrix
				renderMatrix.identity();

				// Calculate the scale
				float scale = MathUtil.min(window.getWidth() / camera.getTargetRatio(), window.getHeight());

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
		if(transformCache == null) {
			return -1;
		}

		for(int i = 0; i < renderCache_alloc.length; i++) {
			if(!renderCache_alloc[i]) {
				renderCache_alloc[i] = true;

				Logger.debug("Allocated render cache at index " + i);

				return i;
			}
		}
		return -1;
	}

	public void deallocCache(int index) {
		if((transformCache == null || velocityCache == null) || index < 0 || index > renderCache_alloc.length) {
			return;
		}

		Logger.debug("Deallocated render cache at index " + index);

		renderCache_alloc[index] = false;
	}

	public Transform getTransformCache(int index) {
		return transformCache[index];
	}

	public Transform getVelocityCache(int index) {
		return velocityCache[index];
	}

	public boolean isFrameFlag() {
		return renderCache_frameflag;
	}

	public Object getFrameLock() {
		return renderCache_framelock;
	}

	public long getCacheUpdateDelta() {
		return currentTime - lastCacheUpdate;
	}

	public void markCacheUpdate() {
		lastCacheUpdate = currentTime;
	}

}