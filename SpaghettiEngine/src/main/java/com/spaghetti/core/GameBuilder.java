package com.spaghetti.core;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.input.InputDispatcher;
import com.spaghetti.input.UpdaterComponent;
import com.spaghetti.networking.ClientComponent;
import com.spaghetti.networking.ServerComponent;
import com.spaghetti.networking.tcp.TCPClient;
import com.spaghetti.networking.tcp.TCPServer;
import com.spaghetti.render.RendererComponent;
import com.spaghetti.settings.GameSettings;
import com.spaghetti.utils.Logger;
import com.spaghetti.world.GameState;

public final class GameBuilder {

	private Class<? extends UpdaterComponent> updater;
	private Class<? extends RendererComponent> renderer;
	private Class<? extends ClientComponent> client;
	private Class<? extends ServerComponent> server;

	private Class<? extends EventDispatcher> eventDispatcherClass = EventDispatcher.class;
	private Class<? extends GameSettings> gameOptionsClass = GameSettings.class;
	private Class<? extends AssetManager> assetManagerClass = AssetManager.class;
	private Class<? extends InputDispatcher> inputDispatcherClass = InputDispatcher.class;
	private Class<? extends ClientState> clientStateClass = ClientState.class;
	private Class<? extends GameState> gameStateClass = GameState.class;
	private Class<? extends Logger> loggerClass = Logger.class;

	public GameBuilder setUpdater(Class<? extends UpdaterComponent> updater) {
		this.updater = updater;
		return this;
	}

	public Class<? extends UpdaterComponent> getUpdater() {
		return updater;
	}

	public GameBuilder enableUpdater() {
		updater = UpdaterComponent.class;
		return this;
	}

	public GameBuilder setRenderer(Class<? extends RendererComponent> renderer) {
		this.renderer = renderer;
		return this;
	}

	public Class<? extends RendererComponent> getRenderer() {
		return renderer;
	}

	public GameBuilder enableRenderer() {
		renderer = RendererComponent.class;
		return this;
	}

	public GameBuilder setClient(Class<? extends ClientComponent> client) {
		this.client = client;
		return this;
	}

	public Class<? extends ClientComponent> getClient() {
		return client;
	}

	public GameBuilder enableClient() {
		client = TCPClient.class;
		return this;
	}

	public GameBuilder setServer(Class<? extends ServerComponent> server) {
		this.server = server;
		return this;
	}

	public Class<? extends ServerComponent> getServer() {
		return server;
	}

	public GameBuilder enableServer() {
		server = TCPServer.class;
		return this;
	}

	public GameBuilder setEventDispatcherClass(Class<? extends EventDispatcher> cls) {
		this.eventDispatcherClass = cls;
		return this;
	}

	public Class<? extends EventDispatcher> getEventDispatcherClass() {
		return eventDispatcherClass;
	}

	public GameBuilder setGameOptionsClass(Class<? extends GameSettings> cls) {
		this.gameOptionsClass = cls;
		return this;
	}

	public Class<? extends GameSettings> getGameOptionsClass() {
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

	public GameBuilder setGameStateClass(Class<? extends GameState> cls) {
		this.gameStateClass = cls;
		return this;
	}

	public Class<? extends GameState> getGameStateClass() {
		return gameStateClass;
	}

	public GameBuilder setLoggerClass(Class<? extends Logger> cls) {
		this.loggerClass = cls;
		return this;
	}

	public Class<? extends Logger> getLoggerClass() {
		return loggerClass;
	}

	public Game build() {
		return new Game(updater, renderer, client, server, eventDispatcherClass, gameOptionsClass, assetManagerClass,
				inputDispatcherClass, clientStateClass, gameStateClass, loggerClass);
	}

}
