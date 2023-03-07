package com.spaghetti.demo;

import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;
import com.spaghetti.networking.tcp.TCPClient;
import com.spaghetti.networking.tcp.TCPServer;
import com.spaghetti.render.RendererCore;

public class MultiplayerDemo {

	public static Game client1, client2;
	public static Game server;

	public static void main(String[] args) {
		// Build clients
		GameBuilder clientBuilder = new GameBuilder();
		clientBuilder.setRenderer(RendererCore.class);
		clientBuilder.setUpdater(MyUpdater.class);
		clientBuilder.setClient(TCPClient.class);

		client1 = clientBuilder.build();
		client2 = clientBuilder.build();

		// Build server
		GameBuilder serverBuilder = new GameBuilder();
		serverBuilder.setUpdater(MyUpdater.class);
		serverBuilder.setServer(TCPServer.class);

		server = serverBuilder.build();

		// Set custom window size before initializing
		Vector2i resolution1 = client1.getEngineOption("resolution");
		int width1 = (int) (resolution1.x * 0.3f);
		int height1 = (int) (resolution1.y * 0.3f);

		Vector2i size1 = client1.getEngineOption("windowsize");
		size1.x = width1;
		size1.y = height1;

		client1.setEngineOption("windowfullscreen", false);

		Vector2i resolution2 = client2.getEngineOption("resolution");
		int width2 = (int) (resolution2.x * 0.3f);
		int height2 = (int) (resolution2.y * 0.3f);

		Vector2i size2 = client2.getEngineOption("windowsize");
		size2.x = width2;
		size2.y = height2;

		client2.setEngineOption("windowfullscreen", false);

		/*
		 * Register key listener that connects to the server when pressing 'P' and
		 * disconnects when pressing 'T'
		 */
		client1.getInputDispatcher().registerListener(new MyKeyListener());
		client2.getInputDispatcher().registerListener(new MyKeyListener());

		// Set the server to depend on the client
		server.depend(client1);
		client1.depend(client2);
		client2.depend(server);

		// Initialize
		client1.begin();
		client2.begin();
		server.begin();

	}

}
