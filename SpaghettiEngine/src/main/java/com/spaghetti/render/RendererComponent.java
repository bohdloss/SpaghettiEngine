package com.spaghetti.render;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.spaghetti.assets.loaders.*;
import com.spaghetti.core.Game;
import com.spaghetti.core.ThreadComponent;
import com.spaghetti.core.events.ExitRequestedEvent;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.utils.*;
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
import com.spaghetti.core.GameThread;
import com.spaghetti.core.GameWindow;
import com.spaghetti.exceptions.GLException;

public class RendererComponent implements ThreadComponent {

	// Internal data
	protected Game game;
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
	protected Vector3f lastCameraPosition = new Vector3f();
	protected Vector3f oldCameraPosition = new Vector3f();
	protected Vector3f lastCameraVelocity = new Vector3f();
	protected Model sceneRenderer;
	protected ShaderProgram defaultShader;

	// 2.1 MB transform cache
	protected final Transform[] transformCache;
	// 2.1 MB velocity cache
	protected final Transform[] velocityCache;
	// 256 KB render cache allocation table
	protected final boolean[] renderCache_alloc;
	// Last cache update
	protected long lastCacheUpdate, lastLastCacheUpdate;
	protected long currentTime;
	// Cache locks
	protected boolean isRendering;
	protected final Object cacheLock = new Object();

	// FPS counter
	protected int fps;
	protected long lastCheck;

	public RendererComponent() {
		window = new GameWindow();

		// Init render cache
		transformCache = new Transform[Short.MAX_VALUE];
		velocityCache = new Transform[Short.MAX_VALUE];
		for(int i = 0; i < transformCache.length; i++) {
			transformCache[i] = new Transform();
			velocityCache[i] = new Transform();
		}
		renderCache_alloc = new boolean[Short.MAX_VALUE];
	}

	@Override
	public void initialize(Game game) throws Throwable {
		this.game = game;

		// Find out if openal needs to be enabled or not
		openal = GameSettings.sgetEngineSetting("openal.enable");
		EventDispatcher.getInstance().registerEventListener(SettingChangedEvent.class, (isClient, event) -> {
			if(event.getSettingName().equals("openal.enable")) {
				openal = (Boolean) event.getNewValue();
				updateOpenAL();
			}
		});

		// Init window and obtain asset manager
		window.winInit(game);
		assetManager = game.getAssetManager();

		// Initializes OpenGL
		window.makeContextCurrent();
		glCapabilities = GL.createCapabilities();
		GL43.glDebugMessageCallback((source, type, id, severity, length, message, userParam) -> {
			String message_str = MemoryUtil.memUTF8(message, length);
			if(severity <= GL43.GL_DEBUG_SEVERITY_NOTIFICATION) {
				Logger.debug(game, "[NOTIFICATION] OpenGL: ");
				Logger.debug(game, "[NOTIFICATION] " + message_str);
			} else if(severity <= GL43.GL_DEBUG_SEVERITY_LOW) {
				Logger.debug(game, "[LOW] OpenGL: ");
				Logger.debug(game, "[LOW] " + message_str);
			} else {
				GLException error = new GLException(source, type, id, severity, message_str);

				// Remove useless lines from stacktrace
				final int useless_lines = 2;
				StackTraceElement[] stack = error.getStackTrace();
				StackTraceElement[] new_stack = new StackTraceElement[stack.length - useless_lines];
				System.arraycopy(stack, useless_lines, new_stack, 0, new_stack.length);
				error.setStackTrace(new_stack);

				if(severity == GL43.GL_DEBUG_SEVERITY_MEDIUM) {
					Logger.error(game, "[MEDIUM] OpenGL: ");
					Logger.error(game, "[MEDIUM] " + message_str, error);
				} else if(severity == GL43.GL_DEBUG_SEVERITY_HIGH) {
					Logger.error(game, "[HIGH] OpenGL: ");
					Logger.error(game, "[HIGH] " + message_str, error);
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
			initOpenAL();
		}

		// Load asset sheets
		assetManager.loadAssetSheet(game.getEngineSetting("assets.internalSheet"));
		assetManager.loadAssetSheet(game.getEngineSetting("assets.assetSheet"));

		GL11.glEnable(GL11.GL_TEXTURE_2D);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glDepthFunc(GL11.GL_LEQUAL);
		GL11.glDisable(GL11.GL_LIGHTING);
		GLFW.glfwSwapInterval(1);

		sceneRenderer = new Model(new float[] { -1f, 1f, 0f, 1f, 1f, 0f, 1f, -1f, 0f, -1f, -1f, 0 },
				new float[] { 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f }, new float[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
				new int[] { 0, 1, 2, 2, 3, 0 });

		defaultShader = ShaderProgram.require("rendererSP");
	}

	protected void updateOpenAL() {
		if(openal && alContext == 0) {
			initOpenAL();
		} else if(!openal && alContext != 0) {
			destroyOpenAL();
		}
	}

	protected void initOpenAL() {
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

	protected void destroyOpenAL() {
		// Unregister audio loaders
		assetManager.unregisterAssetLoader("Music");
		assetManager.unregisterAssetLoader("Sound");

		// Terminates OpenAL
		ALC10.alcMakeContextCurrent(0);
		ALC10.alcDestroyContext(alContext);
		ALC10.alcCloseDevice(alOutputDevice);
		alContext = 0;
	}

	@Override
	public void postInitialize() throws Throwable {
//		for(String device : MicrophoneInputStream.getCaptureDevices()) {
//			Logger.info(device);
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
//		getGame().getLevel("myWorld").addObject(source);
	}

	@Override
	public void terminate() throws Throwable {
		if (openal) {
			destroyOpenAL();
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
	}

	@Override
	public void loop(float delta) throws Throwable {
		currentTime += (long) (delta * 1000f);
		try {
			// Clear screen
			GL11.glGetError();
			GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT | GL11.GL_STENCIL_BUFFER_BIT);

			// Check if the window should be closed
			if (window.shouldClose()) {
				ExitRequestedEvent event = new ExitRequestedEvent();
				game.getEventDispatcher().raiseEvent(event);

				// Event cancelled, do not close the window
				if(event.isCancelled()) {
					window.setShouldClose(false);
				} else {
					game.stopAsync();
				}
			}

			// Render the world
			Camera camera = game.getLocalCamera();
			if (camera != null) {
				renderCamera(delta, camera);
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
	public void preTerminate() throws Throwable {
	}

	protected void renderCamera(float delta, Camera camera) {
		synchronized (getCacheLock()) {
			Transform transform = new Transform();
			int camera_index = camera.getRenderCacheIndex();
			if (camera_index == -1) {
				camera.getWorldTransform(transform);
			} else {
				Transform trans = game.getRenderer().getTransformCache(camera_index);
				Transform vel = game.getRenderer().getVelocityCache(camera_index);
				float velDelta = game.getRenderer().getCacheUpdateDelta();

				vel.position.mul(velDelta, transform.position);
				transform.position.add(trans.position);

				vel.rotation.mul(velDelta, transform.rotation);
				transform.rotation.add(trans.rotation);

				transform.scale.set(0);
				vel.scale.mul(velDelta, transform.scale);
				transform.scale.add(trans.scale);
			}

			// Update position and velocity of OpenAL listener
			if (openal) {
				if (camera != lastCamera) {
					lastCamera = camera;
					oldCameraPosition.set(transform.position);
				}
				lastCameraPosition = transform.position;
				lastCameraPosition.sub(oldCameraPosition, lastCameraVelocity);

				AL10.alListener3f(AL10.AL_POSITION, lastCameraPosition.x, lastCameraPosition.y, lastCameraPosition.z);
				if (delta != 0) {
					lastCameraVelocity.div(delta);
					AL10.alListener3f(AL10.AL_VELOCITY, lastCameraVelocity.x, lastCameraVelocity.y, lastCameraVelocity.z);
				}
				oldCameraPosition.set(lastCameraPosition);
			}

			// Draw level to camera frame buffer
			isRendering = true;
			camera.render(null, delta, transform);
			isRendering = false;
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

	// Getters and setters

	public GameWindow getWindow() {
		return window;
	}

	public boolean isOpenALEnabled() {
		return openal;
	}

	public void setOpenALEnabled(boolean value) {
		GameSettings.ssetEngineSetting("openal.enable", value);
	}

	public int allocCache() {
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
		if(index < 0 || index > renderCache_alloc.length) {
			return;
		}

		renderCache_alloc[index] = false;
		Logger.debug("Deallocated render cache at index " + index);
	}

	public Transform getTransformCache(int index) {
		return transformCache[index];
	}

	public Transform getVelocityCache(int index) {
		return velocityCache[index];
	}

	public boolean isRendering() {
		return isRendering;
	}

	public Object getCacheLock() {
		return cacheLock;
	}

	public long getCacheUpdateDelta() {
		return currentTime - lastCacheUpdate;
	}

	public void markCacheUpdate() {
		lastCacheUpdate = currentTime;
	}

}