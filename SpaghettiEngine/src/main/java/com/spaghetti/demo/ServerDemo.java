package com.spaghetti.demo;

import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;

public class ServerDemo {

	public static Game client;
	public static Game server;

	public static void main(String[] args) {
		Game.initialize();

		// Build client
		GameBuilder clientBuilder = new GameBuilder();
		clientBuilder.enableRenderer();
		clientBuilder.enableUpdater();
		clientBuilder.enableClient();

		client = clientBuilder.build();

		// Build server
		GameBuilder serverBuilder = new GameBuilder();
		serverBuilder.enableUpdater();
		serverBuilder.enableServer();

		server = serverBuilder.build();

		// Set custom window size before initializing
		Vector2i resolution = client.getEngineSetting("resolution");
		int width = (int) (resolution.x * 0.7f);
		int height = (int) (resolution.y * 0.7f);

		Vector2i size = client.getEngineSetting("window.size");
		size.x = width;
		size.y = height;

		/*
		 * Register key listener that connects to the server when pressing 'P' and
		 * disconnects when pressing 'T'
		 */
		client.getInputDispatcher().registerListener(new MyKeyListener());

		// Set the server to depend on the client
		server.depend(client);

		// Initialize
		client.beginAsync();
		server.beginAsync();

		Game.idle();
	}

}
