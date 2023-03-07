package com.spaghetti.input;

import org.lwjgl.glfw.GLFW;

import com.spaghetti.core.Game;

public final class Mouse {

	private Mouse() {
	}

	// Auto generated code

	public static final int B1 = GLFW.GLFW_MOUSE_BUTTON_1;
	public static final int B2 = GLFW.GLFW_MOUSE_BUTTON_2;
	public static final int B3 = GLFW.GLFW_MOUSE_BUTTON_3;
	public static final int B4 = GLFW.GLFW_MOUSE_BUTTON_4;
	public static final int B5 = GLFW.GLFW_MOUSE_BUTTON_5;
	public static final int B6 = GLFW.GLFW_MOUSE_BUTTON_6;
	public static final int B7 = GLFW.GLFW_MOUSE_BUTTON_7;
	public static final int B8 = GLFW.GLFW_MOUSE_BUTTON_8;

	public static final int LEFT = GLFW.GLFW_MOUSE_BUTTON_LEFT;
	public static final int RIGHT = GLFW.GLFW_MOUSE_BUTTON_RIGHT;
	public static final int MIDDLE = GLFW.GLFW_MOUSE_BUTTON_MIDDLE;

	public static final int FIRST = GLFW.GLFW_MOUSE_BUTTON_1;
	public static final int LAST = GLFW.GLFW_MOUSE_BUTTON_LAST;

	public static boolean buttonpressed(int button) {
		Game game = Game.getInstance();
		return (game.isServer() || game.isHeadless()) ? false : game.getWindow().mouseDown(button);
	}

}
