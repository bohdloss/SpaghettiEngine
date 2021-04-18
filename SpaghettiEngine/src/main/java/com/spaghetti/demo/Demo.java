package com.spaghetti.demo;

import com.spaghetti.core.*;
import com.spaghetti.networking.*;
import com.spaghetti.render.*;

public class Demo {

	public static Game server;
	public static Game game;

	public static void main(String[] args) {

		try {
//			Utils.sleep(10000);
			game = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).setClient(new Client())
					.build();
			game.getWindow().setSizeLimit(2, 2, 2000, 2000);
			game.getClient().setJoinHandler(new MyJoinHandler());

			server = new GameBuilder().setUpdater(new MyUpdater()).setServer(new Server()).build();
			server.getServer().setJoinHandler(new MyJoinHandler());

			game.depends(server);
			server.depends(game);

			server.begin();
			game.begin();

			Game.idle();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

}
