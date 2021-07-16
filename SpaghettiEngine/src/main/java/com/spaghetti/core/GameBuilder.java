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

	private Class<? extends UpdaterCore> updater;
	private Class<? extends RendererCore> renderer;
	private Class<? extends ClientCore> client;
	private Class<? extends ServerCore> server;

	private Class<? extends EventDispatcher> eventDispatcherClass = EventDispatcher.class;
	private Class<? extends GameOptions> gameOptionsClass = GameOptions.class;
	private Class<? extends AssetManager> assetManagerClass = AssetManager.class;
	private Class<? extends InputDispatcher> inputDispatcherClass = InputDispatcher.class;
	private Class<? extends ClientState> clientStateClass = ClientState.class;

	public GameBuilder setUpdater(Class<? extends UpdaterCore> updater) {
		this.updater = updater;
		return this;
	}

	public Class<? extends UpdaterCore> getUpdater() {
		return updater;
	}

	public GameBuilder setRenderer(Class<? extends RendererCore> renderer) {
		this.renderer = renderer;
		return this;
	}

	public Class<? extends RendererCore> getRenderer() {
		return renderer;
	}

	public GameBuilder setClient(Class<? extends ClientCore> client) {
		this.client = client;
		return this;
	}

	public Class<? extends ClientCore> getClient() {
		return client;
	}

	public GameBuilder setServer(Class<? extends ServerCore> server) {
		this.server = server;
		return this;
	}

	public Class<? extends ServerCore> getServer() {
		return server;
	}

	public GameBuilder setEventDispatcherClass(Class<? extends EventDispatcher> cls) {
		this.eventDispatcherClass = cls;
		return this;
	}

	public Class<? extends EventDispatcher> getEventDispatcherClass() {
		return eventDispatcherClass;
	}

	public GameBuilder setGameOptionsClass(Class<? extends GameOptions> cls) {
		this.gameOptionsClass = cls;
		return this;
	}

	public Class<? extends GameOptions> getGameOptionsClass() {
		return gameOptionsClass;
	}

	public GameBuilder setAssetManagerClass(Class<? extends AssetManager> cls) {
		this.assetManagerClass = cls;
		return this;
	}

	public Class<? extends AssetManager> getAssetManagerClass() {
		return this.assetManagerClass;
	}

	public GameBuilder setInputDispatcherClass(Class<? extends InputDispatcher> cls) {
		this.inputDispatcherClass = cls;
		return this;
	}

	public Class<? extends InputDispatcher> getInputDispatcherClass() {
		return inputDispatcherClass;
	}

	public GameBuilder setClientStateClass(Class<? extends ClientState> cls) {
		this.clientStateClass = cls;
		return this;
	}

	public Class<? extends ClientState> getClientStateClass() {
		return clientStateClass;
	}

	public Game build() {
		return new Game(updater, renderer, client, server, eventDispatcherClass, gameOptionsClass, assetManagerClass,
				inputDispatcherClass, clientStateClass);
	}

}
