package com.spaghetti.demo;

import com.spaghetti.core.*;
import com.spaghetti.networking.Server;
import com.spaghetti.render.Renderer;

public class Demo {

	public static Game server;
	public static Game game;

	public static void main(String[] args) {

		try {
//			Utils.sleep(10000);
			game = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).build();
			game.getWindow().setSizeLimit(2, 2, 2000, 2000);

//			server = new GameBuilder().setUpdater(new MyUpdater()).setServer(new Server()).build();

//			game.depends(server);
//			server.depends(game);

//			server.begin();
			game.begin();

			Game.idle();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

}
