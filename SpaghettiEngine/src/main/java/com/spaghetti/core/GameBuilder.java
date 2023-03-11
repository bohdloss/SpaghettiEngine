package com.spaghetti.core;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.input.InputDispatcher;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.networking.ClientCore;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.networking.tcp.TCPClient;
import com.spaghetti.networking.tcp.TCPServer;
import com.spaghetti.render.RendererCore;
import com.spaghetti.utils.GameSettings;
import com.spaghetti.utils.Logger;
import com.spaghetti.world.GameState;

public final class GameBuilder {

	private Class<? extends UpdaterCore> updater;
	private Class<? extends RendererCore> renderer;
	private Class<? extends ClientCore> client;
	private Class<? extends ServerCore> server;

	private Class<? extends EventDispatcher> eventDispatcherClass = EventDispatcher.class;
	private Class<? extends GameSettings> gameOptionsClass = GameSettings.class;
	private Class<? extends AssetManager> assetManagerClass = AssetManager.class;
	private Class<? extends InputDispatcher> inputDispatcherClass = InputDispatcher.class;
	private Class<? extends ClientState> clientStateClass = ClientState.class;
	private Class<? extends GameState> gameStateClass = GameState.class;
	private Class<? extends Logger> loggerClass = Logger.class;

	public GameBuilder setUpdater(Class<? extends UpdaterCore> updater) {
		this.updater = updater;
		return this;
	}

	public Class<? extends UpdaterCore> getUpdater() {
		return updater;
	}

	public GameBuilder enableUpdater() {
		updater = UpdaterCore.class;
		return this;
	}

	public GameBuilder setRenderer(Class<? extends RendererCore> renderer) {
		this.renderer = renderer;
		return this;
	}

	public Class<? extends RendererCore> getRenderer() {
		return renderer;
	}

	public GameBuilder enableRenderer() {
		renderer = RendererCore.class;
		return this;
	}

	public GameBuilder setClient(Class<? extends ClientCore> client) {
		this.client = client;
		return this;
	}

	public Class<? extends ClientCore> getClient() {
		return client;
	}

	public GameBuilder enableClient() {
		client = TCPClient.class;
		return this;
	}

	public GameBuilder setServer(Class<? extends ServerCore> server) {
		this.server = server;
		return this;
	}

	public Class<? extends ServerCore> getServer() {
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
