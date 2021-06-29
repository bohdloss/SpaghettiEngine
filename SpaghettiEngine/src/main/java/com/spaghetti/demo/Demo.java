package com.spaghetti.demo;

import com.spaghetti.core.*;
import com.spaghetti.networking.*;
import com.spaghetti.networking.tcp.TCPClient;
import com.spaghetti.networking.tcp.TCPServer;
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

			game = new GameBuilder().setRenderer(new RendererCore()).setUpdater(new MyUpdater()).setClient(new TCPClient())
					.build();			game.getClient().setJoinHandler(new MyJoinHandler());

			game.getInputDispatcher().registerListener(new MyKeyListener());
					
//			game2 = new GameBuilder().setRenderer(new Renderer()).setUpdater(new MyUpdater()).setClient(new Client())
//					.build();
//			game2.getClient().setJoinHandler(new MyJoinHandler());

			server = new GameBuilder().setUpdater(new MyUpdater()).setServer(new TCPServer()).build();
			server.getServer().setJoinHandler(new MyJoinHandler());

//			game.depends(server);
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
