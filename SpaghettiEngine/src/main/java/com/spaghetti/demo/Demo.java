package com.spaghetti.demo;

import com.spaghetti.core.*;
import com.spaghetti.networking.Client;
import com.spaghetti.networking.Server;
import com.spaghetti.render.*;

public class Demo {

	public static Game server;
	public static Game game, game2;

	public static void main(String[] args) {

		try {
//			Utils.sleep(20000);
			GameWindow.defaultMaximumWidth = 2000;
			GameWindow.defaultMaximumHeight = 2000;
			GameWindow.defaultMinimumWidth = 2;
			GameWindow.defaultMinimumHeight = 2;

			game = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).setClient(new Client())
					.build();			game.getClient().setJoinHandler(new MyJoinHandler());

//			game2 = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).setClient(new Client())
//					.build();
//			game2.getClient().setJoinHandler(new MyJoinHandler());

			server = new GameBuilder().setUpdater(new MyUpdater()).setServer(new Server()).build();
			server.getServer().setJoinHandler(new MyJoinHandler());

			game.depends(server);
//			game2.depends(server);

			server.begin();
			game.begin();
//			game2.begin();

			Game.idle();

		} catch (Throwable e) {
			e.printStackTrace();
		}

	}

}
