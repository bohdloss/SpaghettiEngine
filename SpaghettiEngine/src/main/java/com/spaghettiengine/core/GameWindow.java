package com.spaghettiengine.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL11;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.utils.*;

public final class GameWindow {

	// Static fields and methods

	public static int defaultWidth = 400, defaultHeight = 400;
	public static boolean defaultFullscreen;
	public static int defaultMinimumWidth = 100, defaultMinimumHeight = 100;
	public static int defaultMaximumWidth = 800, defaultMaximumHeight = 800;
	public static boolean defaultResizable = true;

	public static void pollEvents() {
		GLFW.glfwPollEvents();
	}

	// Instance fields and methods

	protected String title;
	protected boolean fullscreen;
	protected GLFWImage icon, iconSmall;
	protected long id;
	protected boolean visible;
	protected int minWidth, minHeight, maxWidth, maxHeight;
	protected int width, height;
	protected Game source;

	// Cache
	private int[] intx = new int[1];
	private int[] inty = new int[1];
	private double[] doublex = new double[1];
	private double[] doubley = new double[1];
	private GLFWImage.Buffer iconBuf = GLFWImage.malloc(2);

	public GameWindow(String title, Game source) {
		quickQueue(() -> {
			winInit(title, source);
			return null;
		});
	}

	private void winInit(String title, Game source) {
		this.source = source;
		// Cannot be instantiated outside of a game's context
		if (source == null || source.renderer == null) {
			throw new UnsupportedOperationException();
		}
		this.title = title;
		this.fullscreen = defaultFullscreen;
		this.minWidth = defaultMinimumWidth;
		this.minHeight = defaultMinimumHeight;
		this.maxWidth = defaultMaximumWidth;
		this.maxHeight = defaultMaximumHeight;

		// GLFW native window initialization

		id = GLFW.glfwCreateWindow(defaultWidth, defaultHeight, title, 0, 0);
		if (id == 0) {
			// In case the window does not initialize properly
			throw new IllegalStateException("GLFW window initialization failed");
		}

		GameWindow self = this;

		GLFW.glfwSetWindowSizeCallback(id, new GLFWWindowSizeCallback() {
			@Override
			public void invoke(long window, int width, int height) {
				self.width = width;
				self.height = height;

				Level l = source.getActiveLevel();
				if (l != null) {
					Camera c = l.activeCamera;
					if (c != null) {
						c.setOrtho(width, height);
						c.calcScale();
					}
				}

				Function queue = new Function(() -> {
					GL11.glViewport(0, 0, self.width, self.height);
					GL11.glOrtho(-self.width / 2, self.width / 2, -self.height / 2, self.height / 2, -1, 1);
					return null;
				});

				source.getFunctionDispatcher().queue(queue, self.source.getRendererId(), true);

			}
		});
		gatherSize();
		center();

		GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);

		if (fullscreen) {
			toggleFullscreen(fullscreen);
		}
	}

	public GameWindow(Game source) {
		this("Spaghetti game", source);
	}

	// Gather new size
	private void gatherSize() {
		quickQueue(() -> {
			GLFW.glfwGetWindowSize(id, intx, inty);
			width = intx[0];
			height = inty[0];
			return null;
		});
	}

	// Wrap native functions

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setWidth(int width) {
		quickQueue(() -> {
			GLFW.glfwSetWindowSize(id, width, this.height);
			return null;
		});
	}

	public void setSize(int width, int height) {
		quickQueue(() -> {
			GLFW.glfwSetWindowSize(id, width, height);
			return null;
		});
	}

	public void setHeight(int height) {
		quickQueue(() -> {
			GLFW.glfwSetWindowSize(id, this.width, height);
			return null;
		});
	}

	public void setVisible(boolean visible) {
		quickQueue(() -> {
			if (visible) {
				GLFW.glfwShowWindow(id);
			} else {
				GLFW.glfwHideWindow(id);
			}

			this.visible = visible;
			return null;
		});

	}

	public boolean getVisible() {
		return visible;
	}

	public boolean shouldClose() {
		return GLFW.glfwWindowShouldClose(id);
	}

	public void setShouldClose(boolean close) {
		quickQueue(() -> {
			GLFW.glfwSetWindowShouldClose(id, close);
			return null;
		});
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		quickQueue(() -> {
			GLFW.glfwSetWindowTitle(id, title);

			this.title = title;
			return null;
		});
	}

	public boolean keyDown(int keycode) {
		return (Boolean) quickQueue(() -> (GLFW.glfwGetKey(id, keycode) == GLFW.GLFW_PRESS));
	}

	public boolean mouseDown(int keycode) {
		return (Boolean) quickQueue(() -> (GLFW.glfwGetMouseButton(id, keycode) == GLFW.GLFW_PRESS));
	}

	public double getMouseX() {
		return (Double) quickQueue(() -> {
			GLFW.glfwGetCursorPos(id, doublex, doubley);

			return doublex[0];
		});
	}

	public double getMouseY() {
		return (Double) quickQueue(() -> {
			GLFW.glfwGetCursorPos(id, doublex, doubley);

			return doubley[0];
		});
	}

	public int getX() {
		return (Integer) quickQueue(() -> {
			GLFW.glfwGetWindowPos(id, intx, inty);

			return intx[0];
		});
	}

	public int getY() {
		return (Integer) quickQueue(() -> {
			GLFW.glfwGetWindowPos(id, intx, inty);
			return inty[0];
		});
	}

	public void setX(int x) {
		quickQueue(() -> {
			GLFW.glfwSetWindowPos(id, x, getY());
			return null;
		});
	}

	public void setY(int y) {
		quickQueue(() -> {
			GLFW.glfwSetWindowPos(id, getX(), y);
			return null;
		});
	}

	public int getMinWidth() {
		return minWidth;
	}

	public int getMinHeight() {
		return minHeight;
	}

	public int getMaxWidth() {
		return maxWidth;
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setSizeLimit(int minWidth, int minHeight, int maxWidth, int maxHeight) {
		quickQueue(() -> {
			GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);

			this.minWidth = minWidth;
			this.minHeight = minHeight;
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
			return null;
		});
	}

	public void destroy() {
		quickQueue(() -> {
			GLFW.glfwDestroyWindow(id);
			return null;
		});
	}

	public void close() {
		quickQueue(() -> {
			GLFW.glfwSetWindowShouldClose(id, true);
			return null;
		});
	}

	public void swap() {
		// This absolutely cannot be queued
		// Must be fast enough to run every frame

		GLFW.glfwSwapBuffers(id);
	}

	public void toggleFullscreen(boolean fullscreen) {
		quickQueue(() -> {
			GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

			int width = 1, height = 1;

			if (!fullscreen) {
				width = (int) (mode.width() * 0.7d);
				height = (int) (mode.height() * 0.7d);
			} else {
				width = mode.width();
				height = mode.height();
			}
			GLFW.glfwSetWindowMonitor(id, !fullscreen ? 0 : GLFW.glfwGetPrimaryMonitor(), 0, 0, width, height,
					GLFW.GLFW_DONT_CARE);
			center();
			this.fullscreen = fullscreen;
			return null;
		});
	}

	public void center() {
		quickQueue(() -> {
			GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			GLFW.glfwSetWindowPos(id, (int) (mode.width() * 0.5 - width * 0.5),
					(int) (mode.height() * 0.5 - height * 0.5));
			return null;
		});
	}

	public void setIcon(GLFWImage icon, GLFWImage iconSmall) {
		quickQueue(() -> {
			iconBuf.put(0, icon);
			iconBuf.put(1, iconSmall);
			GLFW.glfwSetWindowIcon(id, iconBuf);
			return null;
		});
	}

	public void makeContextCurrent() {
		// To queue this doesn't make any sense

		GLFW.glfwMakeContextCurrent(id);
	}

	private Object quickQueue(FuncAction action) {
		try {
			long funcId = Game.handler.dispatcher.queue(new Function(action));
			return Game.handler.dispatcher.waitReturnValue(funcId);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

}