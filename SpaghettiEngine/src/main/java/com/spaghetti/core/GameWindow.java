package com.spaghetti.core;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL11;

import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

public final class GameWindow {

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
	protected int iwidth, iheight;
	protected int width, height;
	protected Game source;
	protected boolean async;

	// Cache
	private int[] intx = new int[1];
	private int[] inty = new int[1];
	private double[] doublex = new double[1];
	private double[] doubley = new double[1];
	private GLFWImage.Buffer iconBuf = GLFWImage.malloc(2);

	public GameWindow(String title) {
		this.title = title == null ? "Spaghetti game" : title;
	}

	public GameWindow() {
		this(null);
	}

	public void winInit(Game game) {
		quickQueue(() -> {
			// Cannot be instantiated outside of a game's context
			this.source = game;
			if (source == null || source.getRenderer() == null) {
				throw new UnsupportedOperationException();
			}

			// Retrieve options
			this.fullscreen = game.getEngineOption("defaultfullscreen");
			this.minWidth = game.getEngineOption("defaultminimumwidth");
			this.minHeight = game.getEngineOption("defaultminimumheight");
			this.maxWidth = game.getEngineOption("defaultmaximumwidth");
			this.maxHeight = game.getEngineOption("defaultmaximumheight");
			this.width = game.getEngineOption("defaultwidth");
			this.height = game.getEngineOption("defaultheight");
			this.iwidth = width;
			this.iheight = height;

			// GLFW native window initialization

			id = GLFW.glfwCreateWindow(width, height, title, 0, 0);
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
					if (!fullscreen) {
						self.iwidth = width;
						self.iheight = iheight;
					}
					Camera c = source.getActiveCamera();
					if (c != null) {
						c.calcScale();
					}

					Function queue = () -> {
						GL11.glViewport(0, 0, self.width, self.height);
						GL11.glOrtho(-self.width / 2, self.width / 2, -self.height / 2, self.height / 2, -1, 1);
						return null;
					};

					source.getRendererDispatcher().queue(queue);

				}
			});
			GLFW.glfwSetScrollCallback(id, new GLFWScrollCallback() {

				@Override
				public void invoke(long window, double xoffset, double yoffset) {
					source.getInputDispatcher().scroll = (float) yoffset;
				}

			});
			gatherSize();
			center();
			GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);
			setFullscreen(fullscreen);
			return null;
		});
	}

	// Gather new size
	private void gatherSize() {
		GLFW.glfwGetWindowSize(id, intx, inty);
		width = intx[0];
		height = inty[0];
		if (!fullscreen) {
			iwidth = width;
			iheight = height;
		}
	}

	// Wrap native functions

	public void getSize(Vector2i pointer) {
		pointer.x = width;
		pointer.y = height;
	}

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
		return GLFW.glfwGetKey(id, keycode) == GLFW.GLFW_PRESS;
	}

	public boolean mouseDown(int keycode) {
		return GLFW.glfwGetMouseButton(id, keycode) == GLFW.GLFW_PRESS;
	}

	public int getMouseX() {
		GLFW.glfwGetCursorPos(id, doublex, doubley);
		return (int) doublex[0];
	}

	public int getMouseY() {
		GLFW.glfwGetCursorPos(id, doublex, doubley);
		return (int) doubley[0];
	}

	public void getMousePosition(Vector2i pointer) {
		GLFW.glfwGetCursorPos(id, doublex, doubley);
		pointer.x = (int) doublex[0];
		pointer.y = (int) doubley[0];
	}

	public int getX() {
		GLFW.glfwGetWindowPos(id, intx, inty);
		return intx[0];
	}

	public int getY() {
		GLFW.glfwGetWindowPos(id, intx, inty);
		return inty[0];
	}

	public void getPosition(Vector2i pointer) {
		GLFW.glfwGetWindowPos(id, intx, inty);
		pointer.x = intx[0];
		pointer.y = inty[0];
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
			iconBuf.free();
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
		GLFW.glfwSwapBuffers(id);
	}

	public void setFullscreen(boolean fullscreen) {
		quickQueue(() -> {
			if (this.fullscreen != fullscreen) {
				this.fullscreen = fullscreen;
			} else {
				return null;
			}
			GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

			int width = 1, height = 1;

			if (fullscreen) {
				width = mode.width();
				height = mode.height();
			} else {
				width = iwidth;
				height = iheight;
			}
			GLFW.glfwSetWindowMonitor(id, fullscreen ? GLFW.glfwGetPrimaryMonitor() : 0, 0, 0, width, height,
					GLFW.GLFW_DONT_CARE);
			center();
			return null;
		});
	}

	public void toggleFullscreen() {
		setFullscreen(!fullscreen);
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
		GLFW.glfwMakeContextCurrent(id);
	}

	public boolean isFocused() {
		return GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_FOCUSED) == 1;
	}

	public boolean isFullscreen() {
		return fullscreen;
	}

	private Object quickQueue(Function action) {
		try {
			long funcId = Game.handler.dispatcher.queue(action);
			return async ? null : Game.handler.dispatcher.waitReturnValue(funcId);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

}