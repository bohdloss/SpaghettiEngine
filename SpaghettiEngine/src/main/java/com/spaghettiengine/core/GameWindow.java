package com.spaghettiengine.core;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWWindowSizeCallback;

public final class GameWindow implements Tickable{

//Static fields and methods

public static int defaultWidth = 400, defaultHeight = 400;
public static boolean defaultFullscreen;
public static int defaultMinimumWidth = 100, defaultMinimumHeight = 100;
public static int defaultMaximumWidth = 800, defaultMaximumHeight = 800;
public static boolean defaultResizable=true;

public static void pollEvents() {
	GLFW.glfwPollEvents();
}

//Instance fields and methods

protected String title;
protected boolean fullscreen;
protected GLFWImage icon;
protected long id;
protected boolean visible;
protected int minWidth, minHeight, maxWidth, maxHeight;

//Cache
private int[] intx = new int[1];
private int[] inty = new int[1];
private double[] doublex = new double[1];
private double[] doubley = new double[1];
private GLFWImage.Buffer iconBuf = GLFWImage.malloc(2);

public GameWindow(String title) {
	this.title = title;
	this.fullscreen = defaultFullscreen;
	this.minWidth = defaultMinimumWidth;
	this.minHeight = defaultMinimumHeight;
	this.maxWidth = defaultMaximumWidth;
	this.maxHeight = defaultMaximumHeight;
	
	//GLFW native window initialization
	
	id = GLFW.glfwCreateWindow(defaultWidth, defaultHeight, title, 0, 0);
	if(id == 0) {
		//In case the window does not initialize properly
		throw new IllegalStateException("GLFW window initialization failed");
	}
	
	GLFW.glfwSetWindowSizeCallback(id, new GLFWWindowSizeCallback() {
		@Override
		public void invoke(long window, int width, int height) {
			
		}
	});
	
	GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);
	GLFW.glfwMakeContextCurrent(id);
	
	if(fullscreen) toggleFullscreen(fullscreen);
}

public GameWindow() {
	this("Spaghetti game");
}

//Wrap native functions

public int getWidth() {
	GLFW.glfwGetWindowSize(id, intx, inty);
	return intx[0];
}

public int getHeight() {
	GLFW.glfwGetWindowSize(id, intx, inty);
	return inty[0];
}

public void setWidth(int width) {
	GLFW.glfwSetWindowSize(id, width, getHeight());
}

public void setHeight(int height) {
	GLFW.glfwSetWindowSize(id, getWidth(), height);
}

public void setVisible(boolean visible) {
	if(visible) GLFW.glfwShowWindow(id);
	else GLFW.glfwHideWindow(id);
	
	this.visible = visible;
}

public boolean getVisible() {
	return visible;
}

public boolean shouldClose() {
	return GLFW.glfwWindowShouldClose(id);
}

public String getTitle() {
	return title;
}

public void setTitle(String title) {
	GLFW.glfwSetWindowTitle(id, title);
	
	this.title = title;
}

public boolean keyDown(int keycode) {
	return GLFW.glfwGetKey(id, keycode) == GLFW.GLFW_PRESS;
}

public boolean mouseDown(int keycode) {
	return GLFW.glfwGetMouseButton(id, keycode) == GLFW.GLFW_PRESS;
}

public double getMouseX() {
	GLFW.glfwGetCursorPos(id, doublex, doubley);
	
	return doublex[0];
}

public double getMouseY() {
	GLFW.glfwGetCursorPos(id, doublex, doubley);
	
	return doubley[0];
}

public int getX() {
	GLFW.glfwGetWindowPos(id, intx, inty);
	
	return intx[0];
}

public int getY() {
	GLFW.glfwGetWindowPos(id, intx, inty);
	
	return inty[0];
}

public void setX(int x) {
	GLFW.glfwSetWindowPos(id, x, getY());
}

public void setY(int y) {
	GLFW.glfwSetWindowPos(id, getX(), y);
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
	GLFW.glfwSetWindowSizeLimits(id, minWidth, minHeight, maxWidth, maxHeight);
	
	this.minWidth = minWidth;
	this.minHeight = minHeight;
	this.maxWidth = maxWidth;
	this.maxHeight = maxHeight;
}

public void destroy() {
	GLFW.glfwDestroyWindow(id);
}

public void close() {
	GLFW.glfwSetWindowShouldClose(id, true);
}

public void swap() {
	GLFW.glfwSwapBuffers(id);
}

public void toggleFullscreen(boolean fullscreen) {
	GLFWVidMode mode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
	
	int width = 1, height = 1;

	if(!fullscreen) {
		width=(int)((double)mode.width()*0.7d);
		height=(int)((double)mode.height()*0.7d);
	} else {
		width=mode.width();
		height=mode.height();
	}
	GLFW.glfwSetWindowMonitor(id, !fullscreen ? 0 : GLFW.glfwGetPrimaryMonitor(), 0, 0, width, height, GLFW.GLFW_DONT_CARE);
	GLFW.glfwSetWindowPos(id, (int)((double)mode.width()*0.5-(double)width*0.5), (int)((double)mode.height()*0.5-(double)height*0.5));
	this.fullscreen=fullscreen;
}

public void setIcon(GLFWImage icon, GLFWImage iconSmall) {
	iconBuf.put(0, icon);
	iconBuf.put(1, iconSmall);
	GLFW.glfwSetWindowIcon(id, iconBuf);
}

@Override
public void update(float delta) {}

}