package com.spaghetti.demo;

import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;
import com.spaghetti.networking.tcp.TCPClient;
import com.spaghetti.networking.tcp.TCPServer;
import com.spaghetti.render.RendererCore;

public class ServerDemo {

	public static Game client;
	public static Game server;

	public static void main(String[] args) {
		// Build client
		GameBuilder clientBuilder = new GameBuilder();
		clientBuilder.setRenderer(RendererCore.class);
		clientBuilder.setUpdater(MyUpdater.class);
		clientBuilder.setClient(TCPClient.class);

		client = clientBuilder.build();

		// Build server
		GameBuilder serverBuilder = new GameBuilder();
		serverBuilder.setUpdater(MyUpdater.class);
		serverBuilder.setServer(TCPServer.class);

		server = serverBuilder.build();

		// Set custom window size before initializing
		Vector2i resolution = client.getEngineOption("resolution");
		int width = (int) (resolution.x * 0.7f);
		int height = (int) (resolution.y * 0.7f);

		Vector2i size = client.getEngineOption("windowsize");
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
		client.begin();
		server.begin();

	}

}
