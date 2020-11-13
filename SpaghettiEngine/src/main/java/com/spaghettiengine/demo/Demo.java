package com.spaghettiengine.demo;

import org.lwjgl.glfw.GLFW;

import com.spaghettiengine.core.Game;
import com.spaghettiengine.core.GameWindow;

public class Demo {

	public static void main(String[] args) {
		
		Game.init();
		GameWindow window = new GameWindow();
		window.toggleFullscreen(true);
		
		while(!window.shouldClose()) {
			GameWindow.pollEvents();
			System.out.println(window.keyDown(GLFW.GLFW_KEY_B));
		}
		
		System.out.println("Hello world");
	}

}
