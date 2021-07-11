package com.spaghetti.core;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.input.InputDispatcher;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.networking.ClientCore;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.render.RendererCore;
import com.spaghetti.utils.GameOptions;

public final class GameBuilder {

	private UpdaterCore updater;
	private RendererCore renderer;
	private ClientCore client;
	private ServerCore server;

	private Class<? extends EventDispatcher> eventDispatcherClass = EventDispatcher.class;
	private Class<? extends GameOptions> gameOptionsClass = GameOptions.class;
	private Class<? extends AssetManager> assetManagerClass = AssetManager.class;
	private Class<? extends InputDispatcher> inputDispatcherClass = InputDispatcher.class;
	private Class<? extends ClientState> clientStateClass = ClientState.class;

	private Game game;

	public GameBuilder setUpdater(UpdaterCore updater) {
		if (game != null) {
			return this;
		}
		this.updater = updater;
		return this;
	}

	public UpdaterCore getUpdater() {
		return updater;
	}

	public GameBuilder setRenderer(RendererCore renderer) {
		if (game != null) {
			return this;
		}
		this.renderer = renderer;
		return this;
	}

	public RendererCore getRenderer() {
		return renderer;
	}

	public GameBuilder setClient(ClientCore client) {
		if (game != null) {
			return this;
		}
		this.client = client;
		return this;
	}

	public ClientCore getClient() {
		return client;
	}

	public GameBuilder setServer(ServerCore server) {
		if (game != null) {
			return this;
		}
		this.server = server;
		return this;
	}

	public ServerCore getServer() {
		return server;
	}

	public GameBuilder setEventDispatcherClass(Class<? extends EventDispatcher> cls) {
		if (game != null) {
			return this;
		}
		this.eventDispatcherClass = cls;
		return this;
	}

	public Class<? extends EventDispatcher> getEventDispatcherClass() {
		return eventDispatcherClass;
	}

	public GameBuilder setGameOptionsClass(Class<? extends GameOptions> cls) {
		if (game != null) {
			return this;
		}
		this.gameOptionsClass = cls;
		return this;
	}

	public Class<? extends GameOptions> getGameOptionsClass() {
		return gameOptionsClass;
	}

	public GameBuilder setAssetManagerClass(Class<? extends AssetManager> cls) {
		if (game != null) {
			return this;
		}
		this.assetManagerClass = cls;
		return this;
	}

	public Class<? extends AssetManager> getAssetManagerClass() {
		return this.assetManagerClass;
	}

	public GameBuilder setInputDispatcherClass(Class<? extends InputDispatcher> cls) {
		if (game != null) {
			return this;
		}
		this.inputDispatcherClass = cls;
		return this;
	}

	public Class<? extends InputDispatcher> getInputDispatcherClass() {
		return inputDispatcherClass;
	}

	public GameBuilder setClientStateClass(Class<? extends ClientState> cls) {
		if (game != null) {
			return this;
		}
		this.clientStateClass = cls;
		return this;
	}

	public Class<? extends ClientState> getClientStateClass() {
		return clientStateClass;
	}

	public Game build() throws Throwable {
		return new Game(updater, renderer, client, server, eventDispatcherClass, gameOptionsClass, assetManagerClass,
				inputDispatcherClass, clientStateClass);
	}

	public Game getGame() {
		return game;
	}

}
