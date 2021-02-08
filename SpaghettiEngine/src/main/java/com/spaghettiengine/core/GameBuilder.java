package com.spaghettiengine.core;

import com.spaghettiengine.input.Updater;
import com.spaghettiengine.networking.Client;
import com.spaghettiengine.networking.Server;
import com.spaghettiengine.render.Renderer;

public final class GameBuilder {

	private Updater updater;
	private Renderer renderer;
	private Client client;
	private Server server;

	private Game game;

	public GameBuilder setUpdater(Updater updater) {
		if (game != null) {
			return this;
		}
		this.updater = updater;
		return this;
	}

	public Updater getUpdater() {
		return updater;
	}

	public GameBuilder setRenderer(Renderer renderer) {
		if (game != null) {
			return this;
		}
		this.renderer = renderer;
		return this;
	}

	public Renderer getRenderer() {
		return renderer;
	}

	public GameBuilder setClient(Client client) {
		if (game != null) {
			return this;
		}
		this.client = client;
		return this;
	}

	public Client getClient() {
		return client;
	}

	public GameBuilder setServer(Server server) {
		if(game != null) {
			return this;
		}
		this.server = server;
		return this;
	}
	
	public Server getServer() {
		return server;
	}
	
	public Game build() throws Throwable {
		return new Game(updater, renderer, client, server);
	}

	public Game getGame() {
		return game;
	}

}
