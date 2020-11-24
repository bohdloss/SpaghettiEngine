package com.spaghettiengine.demo;

import com.spaghettiengine.core.*;

public class Demo extends Thread {

	public static void main(String[] args) {

		try {

			Game game = new Game(Updater.class, Renderer.class);
			game.begin();

			Game.idle();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		try {
			Game game2 = new Game(Updater.class, Renderer.class);
			game2.begin();
			game2.getWindow().setTitle("die");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
