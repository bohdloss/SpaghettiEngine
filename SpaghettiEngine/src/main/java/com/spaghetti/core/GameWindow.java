package com.spaghetti.core;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.joml.Vector2i;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import org.lwjgl.opengl.GL11;

import com.spaghetti.utils.Function;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.ImageUtils;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ResourceLoader;

public final class GameWindow {

	public static void pollEvents() {
		GLFW.glfwPollEvents();
	}

	// Instance fields and methods

	protected String title;
	protected long id;
	protected int minWidth, minHeight, maxWidth, maxHeight;
	protected int savedWidth, savedHeight;
	protected int width, height;
	protected Game source;
	protected boolean async;

	// Cache
	private int[] intx = new int[1];
	private int[] inty = new int[1];
	private double[] doublex = new double[1];
	private double[] doubley = new double[1];
	private long cursorId;

	public void winInit(Game game) {
		quickQueue(() -> {
			// Cannot be instantiated outside of a game's context
			this.source = game;
			if (source == null || source.getRenderer() == null) {
				throw new UnsupportedOperationException();
			}

			// Retrieve options
			boolean fullscreen = game.getEngineOption("windowfullscreen");
			boolean resizable = game.getEngineOption("windowresizable");

			Vector2i size = game.getEngineOption("windowsize");
			Vector2i size_min = game.getEngineOption("windowminimumsize");
			Vector2i size_max = game.getEngineOption("windowmaximumsize");

			this.title = game.getEngineOption("windowtitle");

			this.width = size.x;
			this.height = size.y;
			this.minWidth = size_min.x;
			this.minHeight = size_min.y;
			this.maxWidth = size_max.x;
			this.maxHeight = size_max.y;
			this.savedWidth = width;
			this.savedHeight = height;

			// GLFW native window initialization
			boolean debug_context = (boolean) game.getEngineOption("debugcontext");
			GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_DEBUG_CONTEXT, debug_context ? GL11.GL_TRUE : GL11.GL_FALSE);

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

					Camera c = source.getLocalCamera();
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
					source.getInputDispatcher().xscroll = (float) xoffset;
					source.getInputDispatcher().yscroll = (float) yoffset;
				}

			});
			center();
			GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);
			setFullscreen(fullscreen);
			setResizable(resizable);
			setIcon((String) game.getEngineOption("windowicon32"), (String) game.getEngineOption("windowicon16"));
			return null;
		});
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

	public void setResizable(boolean resizable) {
		quickQueue(() -> {
			GLFW.glfwSetWindowAttrib(id, GLFW.GLFW_RESIZABLE, resizable ? GLFW.GLFW_TRUE : GLFW.GLFW_FALSE);
			return null;
		});
	}

	public boolean isResizable() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_RESIZABLE) == GLFW.GLFW_TRUE));
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
				if (isFullscreen()) {
					return null;
				}
				GLFW.glfwHideWindow(id);
			}
			return null;
		});

	}

	public boolean isVisible() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_VISIBLE) == GLFW.GLFW_TRUE));
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

	public int getWindowX() {
		return (int) quickQueue(() -> {
			GLFW.glfwGetWindowPos(id, intx, inty);
			return intx[0];
		});
	}

	public int getWindowY() {
		return (int) quickQueue(() -> {
			GLFW.glfwGetWindowPos(id, intx, inty);
			return inty[0];
		});
	}

	public void getPosition(Vector2i pointer) {
		quickQueue(() -> {
			GLFW.glfwGetWindowPos(id, intx, inty);
			pointer.x = intx[0];
			pointer.y = inty[0];
			return null;
		});
	}

	public void setX(int x) {
		quickQueue(() -> {
			GLFW.glfwSetWindowPos(id, x, getWindowY());
			return null;
		});
	}

	public void setY(int y) {
		quickQueue(() -> {
			GLFW.glfwSetWindowPos(id, getWindowX(), y);
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
			if (cursorId != 0) {
				GLFW.glfwDestroyCursor(cursorId);
			}
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
			if (isFullscreen() == fullscreen) {
				return null;
			}
			GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

			int desiredWidth = 1, desiredHeight = 1;

			if (fullscreen) {
				desiredWidth = mode.width();
				desiredHeight = mode.height();
				savedWidth = width;
				savedHeight = height;
			} else {
				desiredWidth = savedWidth;
				desiredHeight = savedHeight;
			}
			long monitor = fullscreen ? GLFW.glfwGetPrimaryMonitor() : 0;
			int desiredx = fullscreen ? 0 : (mode.width() / 2 - desiredWidth / 2);
			int desiredy = fullscreen ? 0 : (mode.height() / 2 - desiredHeight / 2);
			GLFW.glfwSetWindowMonitor(id, monitor, desiredx, desiredy, desiredWidth, desiredHeight,
					GLFW.GLFW_DONT_CARE);
			return null;
		});
	}

	public void toggleFullscreen() {
		setFullscreen(!isFullscreen());
	}

	public void center() {
		quickQueue(() -> {
			GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
			GLFW.glfwSetWindowPos(id, (int) (mode.width() * 0.5 - width * 0.5),
					(int) (mode.height() * 0.5 - height * 0.5));
			return null;
		});
	}

	public void setIcon(BufferedImage... images) {
		// Allocate an array of images
		GLFWImage.Buffer imgBuf = GLFWImage.malloc(images.length);
		ByteBuffer[] toFree = new ByteBuffer[images.length];

		// Iterate through all images
		for (int i = 0; i < images.length; i++) {
			// Retrieve some basic info
			BufferedImage image = images[i];
			int width = image.getWidth();
			int height = image.getHeight();

			// Convert to ByteBuffer
			ByteBuffer imageData = ImageUtils.parseImage(image, null);
			toFree[i] = imageData;

			// Generate GLFWImage struct
			GLFWImage imageGlfw = GLFWImage.malloc();
			imageGlfw.width(width);
			imageGlfw.height(height);
			imageGlfw.pixels(imageData);

			// Put into buffer
			imgBuf.put(i, imageGlfw);
		}

		// Perform operation
		quickQueue(() -> {
			GLFW.glfwSetWindowIcon(id, imgBuf);
			return null;
		});

		// Free resources
		for (ByteBuffer buffer : toFree) {
			ImageUtils.freeImage(buffer);
		}
		for (GLFWImage img : imgBuf) {
			img.free();
		}
		imgBuf.free();
	}

	public void setIcon(String... paths) {
		try {
			BufferedImage[] images = new BufferedImage[paths.length];
			for (int i = 0; i < images.length; i++) {
				images[i] = ResourceLoader.loadImage(paths[i]);
			}
			setIcon(images);
		} catch (Throwable t) {
			Logger.error(source, "Error loading window icon", t);
		}
	}

	public void setCursor(BufferedImage cursor, int centerX, int centerY, int cursorWidth, int cursorHeight) {
		// Cursors larger than this number glitch out on X11
		final int maxCursorSize = 256;
		if (cursorWidth > maxCursorSize || cursorHeight > maxCursorSize) {
			throw new IllegalArgumentException("Neither width or height of cursor can be larger than " + maxCursorSize);
		}

		// Resize cursor
		BufferedImage resized = new BufferedImage(cursorWidth, cursorHeight, BufferedImage.TYPE_INT_ARGB);
		resized.getGraphics().drawImage(cursor, 0, 0, cursorWidth, cursorHeight, null);
		ByteBuffer imageData = ImageUtils.parseImage(resized, null);

		// Allocate glfw resources
		GLFWImage cursorGlfw = GLFWImage.malloc();
		cursorGlfw.width(cursorWidth);
		cursorGlfw.height(cursorHeight);
		cursorGlfw.pixels(imageData);

		quickQueue(() -> {
			if (cursorId != 0) {
				GLFW.glfwDestroyCursor(cursorId);
			}
			cursorId = GLFW.glfwCreateCursor(cursorGlfw, centerX, centerY);
			GLFW.glfwSetCursor(id, cursorId);
			return null;
		});

		// Free resources
		ImageUtils.freeImage(imageData);
		cursorGlfw.free();
	}

	public void resetCursor() {
		quickQueue(() -> {
			GLFW.glfwSetCursor(id, 0);
			return null;
		});
	}

	public void setCursor(String path, int centerX, int centerY, int cursorWidth, int cursorHeight) {
		try {
			setCursor(ResourceLoader.loadImage(path), centerX, centerY, cursorWidth, cursorHeight);
		} catch (Throwable t) {
			Logger.error(source, "Error loading mouse cursor", t);
		}
	}

	public void makeContextCurrent() {
		GLFW.glfwMakeContextCurrent(id);
	}

	public boolean isFocused() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_FOCUSED) == GLFW.GLFW_TRUE));
	}

	public void requestAttention() {
		quickQueue(() -> {
			GLFW.glfwRequestWindowAttention(id);
			return null;
		});
	}

	public void requestFocus() {
		quickQueue(() -> {
			GLFW.glfwFocusWindow(id);
			return null;
		});
	}

	public boolean isFullscreen() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowMonitor(id) != 0));
	}

	public float getOpacity() {
		return (float) quickQueue(() -> GLFW.glfwGetWindowOpacity(id));
	}

	public void setOpacity(float opacity) {
		quickQueue(() -> {
			GLFW.glfwSetWindowOpacity(id, opacity);
			return null;
		});
	}

	public boolean isIconified() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE));
	}

	public void setIconified(boolean iconified) {
		quickQueue(() -> {
			if (iconified) {
				GLFW.glfwIconifyWindow(id);
			} else {
				GLFW.glfwRestoreWindow(id);
			}
			return null;
		});

	}

	public boolean isMaximized() {
		return (boolean) quickQueue(() -> (GLFW.glfwGetWindowAttrib(id, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE));
	}

	public void setMaximized(boolean maximized) {
		quickQueue(() -> {
			if (maximized) {
				GLFW.glfwMaximizeWindow(id);
			} else {
				GLFW.glfwRestoreWindow(id);
			}
			return null;
		});

	}

	private Object quickQueue(Function action) {
		try {
			long funcId = Game.handlerThread.dispatcher.queue(action);
			return async ? null : Game.handlerThread.dispatcher.waitReturnValue(funcId);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

}