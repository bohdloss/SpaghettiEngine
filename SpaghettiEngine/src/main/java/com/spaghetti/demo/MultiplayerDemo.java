package com.spaghetti.demo;

import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;

public class MultiplayerDemo {

	public static Game client1, client2;
	public static Game server;

	public static void main(String[] args) {
		// Build clients
		GameBuilder clientBuilder = new GameBuilder();
		clientBuilder.enableRenderer();
		clientBuilder.enableUpdater();
		clientBuilder.enableClient();

		client1 = clientBuilder.build();
		client2 = clientBuilder.build();

		// Build server
		GameBuilder serverBuilder = new GameBuilder();
		serverBuilder.enableUpdater();
		serverBuilder.enableServer();

		server = serverBuilder.build();

		// Set custom window size before initializing
		Vector2i resolution1 = client1.getEngineSetting("resolution");
		int width1 = (int) (resolution1.x * 0.3f);
		int height1 = (int) (resolution1.y * 0.3f);

		Vector2i size1 = client1.getEngineSetting("window.size");
		size1.x = width1;
		size1.y = height1;

		client1.setEngineSetting("window.fullscreen", false);

		Vector2i resolution2 = client2.getEngineSetting("resolution");
		int width2 = (int) (resolution2.x * 0.3f);
		int height2 = (int) (resolution2.y * 0.3f);

		Vector2i size2 = client2.getEngineSetting("window.size");
		size2.x = width2;
		size2.y = height2;

		client2.setEngineSetting("window.fullscreen", false);

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
