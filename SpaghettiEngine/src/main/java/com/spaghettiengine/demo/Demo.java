package com.spaghettiengine.demo;

import com.spaghettiengine.core.*;

public class Demo {

	public static void main(String[] args) {
		
		try {
			
			Game.init();
			Game game = new Game(new GameWindow(), Updater.class, Renderer.class);
			game.begin();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

}
