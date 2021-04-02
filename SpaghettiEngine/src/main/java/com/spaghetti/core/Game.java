package com.spaghetti.core;

import java.util.*;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.Signals;
import com.spaghetti.input.Updater;
import com.spaghetti.networking.Client;
import com.spaghetti.networking.Server;
import com.spaghetti.render.Renderer;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.GameOptions;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public final class Game {

	// Support for multiple instances of the engine
	// in the same java process
	protected volatile static ArrayList<Game> games = new ArrayList<>();
	protected volatile static HashMap<Long, Integer> links = new HashMap<>();
	protected volatile static Handler handler;

	static {
		init();
	}

	private static void init() {
		if (handler != null) {
			handler.stop = true;
		}
		handler = new Handler();
		handler.start();
		while (handler.dispatcher == null) {
			Utils.sleep(1);
		}
	}

	// Stop all instances
	public static void stopAll() {
		for (Game current : games) {
			if (!current.isStopped()) {
				current.stop();
			}
		}

		handler.stop = true;
	}

	// Wait for all instances to finish
	public static void waitAll() {
		while (true) {
			Utils.sleep(1);
			boolean found = false;
			for (Game current : games) {
				if (current != null && !current.isStopped()) {
					found = true;
				}
			}
			if (!found) {
				return;
			}
		}
	}

	// Free the main thread
	public static void idle() {
		handler.stopOnNoActivity = true;
	}

	// Static Thread-based methods

	public static Game getGame() {
		Long id = Thread.currentThread().getId();
		Integer index = links.get(id);
		if (index != null) {
			if (index >= 0 && index < games.size()) {
				Game game = games.get(index);
				if (game != null && !game.isStopped()) {
					return game;
				}
			}
		}
		return null;
	}

	// Instance globals
	private volatile GameWindow window;
	private volatile AssetManager assetManager;
	private volatile EventDispatcher eventDispatcher;

	private volatile Updater updater;
	private volatile Renderer renderer;
	private volatile Client client;
	private volatile Server server;

	private volatile int index;
	private volatile boolean stopped;
	private volatile boolean init;

	protected boolean stopSignal;
	protected ArrayList<Game> dependencies = new ArrayList<>();

	// Security
	private volatile boolean starting;
	private volatile boolean stopping;

	// Cached booleans
	private volatile boolean isHeadless;
	private volatile boolean isClient;
	private volatile boolean isMultiplayer;
	private volatile boolean hasAutority;

	private volatile Level activeLevel;
	private volatile GameOptions options;

	// Constructors using custom classes
	public Game(Updater updater, Renderer renderer, Client client, Server server) throws Throwable {

		if (client != null && server != null) {
			throw new IllegalArgumentException("Cannot have both a client and a server in a Game");
		}
		if (server != null && renderer != null) {
			throw new IllegalArgumentException("Cannot have both a server with a renderer in a Game");
		}
		if (updater == null && renderer == null) {
			throw new IllegalArgumentException("At least an updater or a renderer is required in a Game");
		}
		if (updater == null && (client != null || server != null)) {
			throw new IllegalArgumentException("Cannot have a client or a server without an updater in a Game");
		}

		this.eventDispatcher = new EventDispatcher(this);

		this.options = new GameOptions();
		this.assetManager = new AssetManager(this);

		games.add(this);
		this.index = games.indexOf(this);

		if (updater != null) {
			this.updater = updater;
			this.updater.setName("UPDATER");
			registerThread(this.updater);
		}
		if (renderer != null) {
			this.renderer = renderer;
			this.renderer.setName("RENDERER");
			registerThread(this.renderer);
			window = new GameWindow(this);
		}
		if (client != null) {
			this.client = client;
			this.client.setName("CLIENT");
			registerThread(this.client);
		}
		if (server != null) {
			this.server = server;
			this.server.setName("SERVER");
			registerThread(this.server);
		}

		this.isHeadless = window == null || renderer == null;
		this.isMultiplayer = client != null || server != null;
		this.isClient = (client != null || server == null) || !isMultiplayer;
		this.hasAutority = server != null || !isMultiplayer;
	}

	// If the provided game instance dies, this instance does too
	public void depends(Game game) {
		if (!dependencies.contains(game)) {
			dependencies.add(game);
		}
	}

	// Reverts the effects of depends()
	public void not_depends(Game game) {
		dependencies.remove(game);
	}

	// Linking/Unlinking of threads to this game instance

	public void registerThread(Thread t) {
		long id = t.getId();
		if (links.get(id) == null) {
			links.put(id, index);
		}
	}

	public void registerThread() {
		registerThread(Thread.currentThread());
	}

	public void unregisterThread(Thread t) {
		long id = t.getId();
		if (links.get(id) != null && links.get(id) == index) {
			links.remove(id);
		}
	}

	public void unregisterThread() {
		unregisterThread(Thread.currentThread());
	}

	// Start all child threads
	public void begin() throws Throwable {

		if (stopped || init || starting || stopping) {
			return;
		}

		Logger.loading(this, "Allocating assets...");
		starting = true;
		if (assetManager != null) {
			assetManager.loadAssetSheet(options.getOption(GameOptions.PREFIX + "assetsheet"));
		}

		// First start all threads

		Logger.loading(this, "Telling threads to start...");
		if (updater != null) {
			updater.start(this);
		}
		if (renderer != null) {
			renderer.start(this);
		}
		if (client != null) {
			client.start(this);
		}
		if (server != null) {
			server.start(this);
		}

		// Then wait for initialization

		Logger.loading(this, "Waiting for initialization to end...");
		if (updater != null) {
			updater.waitInit();
		}
		if (renderer != null) {
			renderer.waitInit();
		}
		if (client != null) {
			client.waitInit();
		}
		if (server != null) {
			server.waitInit();
		}

		// Mark this instance as initialized

		Logger.loading(this, "Allowing threads to run...");
		if (updater != null) {
			updater.allowRun();
		}
		if (renderer != null) {
			renderer.allowRun();
		}
		if (client != null) {
			client.allowRun();
		}
		if (server != null) {
			server.allowRun();
		}

		init = true;
		starting = false;

		eventDispatcher.raiseSignal((GameObject) null, Signals.SIGSTART);

		Logger.info(this, "Ready!");
	}

	// Getters

	// Stop all child threads and flag this game instance as stopped
	public void stop() {
		if (stopped || stopping || starting) {
			return;
		}
		stopping = true;

		// First send stop signal to all threads

		Logger.info(this, "Telling threads to shut down...");
		if (renderer != null) {
			renderer.terminate();
		}
		if (updater != null) {
			updater.terminate();
		}
		if (client != null) {
			client.terminate();
		}
		if (server != null) {
			server.terminate();
		}

		// Wait for execution to stop

		Logger.info(this, "Waiting for execution to end...");
		if (updater != null) {
			updater.waitExecution();
		}
		if (renderer != null) {
			renderer.waitExecution();
		}
		if (client != null) {
			client.waitExecution();
		}
		if (server != null) {
			server.waitExecution();
		}

		// Raise shutdown signal

		Logger.info(this, "Executing shutdown hooks...");
		eventDispatcher.raiseSignal((GameObject) null, Signals.SIGSTOP);

		// Allow finalization to begin

		Logger.info(this, "Allowing threads to stop...");
		if (updater != null) {
			updater.allowStop();
		}
		if (renderer != null) {
			renderer.allowStop();
		}
		if (client != null) {
			client.allowStop();
		}
		if (server != null) {
			server.allowStop();
		}

		// Then wait for finalization

		Logger.info(this, "Executing gloabl shutdown hooks...");
		if (updater != null) {
			updater.waitTerminate();
		}
		if (renderer != null) {
			renderer.waitTerminate();
		}
		if (client != null) {
			client.waitTerminate();
		}
		if (server != null) {
			server.waitTerminate();
		}

		// Destroy the window if it's there

		if (window != null) {
			window.destroy();
		}

		internal_finishstop();
	}

	protected void internal_finishstop() {
		stopped = true;
		stopping = false;

		Logger.info(this, "Stopped");
		System.gc();
	}

	public void stopAsync() {
		stopSignal = true;
	}

	public boolean isStarting() {
		return starting;
	}

	public boolean isStopping() {
		return stopping;
	}

	public Renderer getRenderer() {
		return renderer;
	}

	public Updater getUpdater() {
		return updater;
	}

	public Client getClient() {
		return client;
	}

	public Server getServer() {
		return server;
	}

	public GameWindow getWindow() {
		return window;
	}

	public EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}

	// Returns true if no renderer or window is associated with this game
	public boolean isHeadless() {
		return isHeadless;
	}

	// Returns true if this game is a client or not multiplayer at all
	public boolean isClient() {
		return isClient;
	}

	// Returns the negative value of isClient()
	public boolean isServer() {
		return !isClient;
	}

	// Returns true if this game is multiplayer
	public boolean isMultiplayer() {
		return isMultiplayer;
	}

	// Returns true if this game is either a server or not multiplayer at all
	public boolean hasAuthority() {
		return hasAutority;
	}

	public boolean isDead() {
		boolean updNull = updater == null;
		boolean renderNull = renderer == null;
		boolean clientNull = client == null;
		boolean serverNull = server == null;

		if (updNull && renderNull && clientNull && serverNull) {
			return true;
		}

		if (!updNull && updater.stopped()) {
			return true;
		}
		if (!renderNull && renderer.stopped()) {
			return true;
		}
		if (!clientNull && client.stopped()) {
			return true;
		}
		if (!serverNull && server.stopped()) {
			return true;
		}

		return false;
	}

	public boolean isStopped() {
		return stopped;
	}

	public boolean isInit() {
		return init;
	}

	public int getIndex() {
		return index;
	}

	public double getTickMultiplier(double delta) {
		return (delta / 1000) / options.getTick();
	}

	public Level getActiveLevel() {
		return activeLevel;
	}

	public long getUpdaterId() {
		return updater.getId();
	}

	public long getRendererId() {
		return renderer.getId();
	}

	public long getClientId() {
		return client.getId();
	}

	public long getServerId() {
		return server.getId();
	}

	public FunctionDispatcher getUpdaterDispatcher() {
		return updater.getDispatcher();
	}

	public FunctionDispatcher getRendererDispatcher() {
		return renderer.getDispatcher();
	}

	public FunctionDispatcher getClientDispatcher() {
		return client.getDispatcher();
	}

	public FunctionDispatcher getServerDispatcher() {
		return server.getDispatcher();
	}

	public AssetManager getAssetManager() {
		return assetManager;
	}

	public GameOptions getOptions() {
		return options;
	}

	public void detachLevel() {
		if (activeLevel == null) {
			return;
		}
		activeLevel.source = null;
		activeLevel = null;
	}

	public void attachLevel(Level level) {
		if (level == null) {
			return;
		}
		if (activeLevel != null) {
			detachLevel();
		}
		activeLevel = level;
		activeLevel.source = this;
	}

}