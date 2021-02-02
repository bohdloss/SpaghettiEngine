package com.spaghettiengine.demo;

import com.spaghettiengine.core.*;
import com.spaghettiengine.render.Renderer;
import com.spaghettiengine.utils.Utils;

public class Demo {

	public static Game game;
	
	public static void main(String[] args) {

		try {
			
			game = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).build();
			game.getWindow().setSizeLimit(2, 2, 2000, 2000);
			game.begin();

			Game.idle();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}
	
}
