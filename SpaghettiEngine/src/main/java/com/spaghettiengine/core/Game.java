package com.spaghettiengine.core;

import java.util.*;

import com.spaghettiengine.assets.AssetManager;
import com.spaghettiengine.input.Updater;
import com.spaghettiengine.networking.Client;
import com.spaghettiengine.networking.Server;
import com.spaghettiengine.render.Renderer;
import com.spaghettiengine.utils.FunctionDispatcher;
import com.spaghettiengine.utils.GameOptions;
import com.spaghettiengine.utils.Logger;
import com.spaghettiengine.utils.Utils;

public final class Game {

	// Support for multiple instances of the engine
	// in the same java process
	protected static ArrayList<Game> games = new ArrayList<>();
	protected static HashMap<Long, Integer> links = new HashMap<>();
	protected static Handler handler;

	static {
		init();
	}

	private static void init() {
		if (handler != null) {
			handler.stop = true;
		}
		handler = new Handler();
		handler.start();
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
	private FunctionDispatcher dispatcher;
	private GameWindow window;
	private AssetManager assetManager;
	private EventDispatcher eventDispatcher;

	private Updater updater;
	private Renderer renderer;
	private Client client;
	private Server server;

	private int index;
	private boolean stopped;
	private boolean init;

	protected boolean stopSignal;
	protected ArrayList<Game> dependencies = new ArrayList<Game>();
	
	// Security
	private boolean starting;
	private boolean stopping;

	// Cached booleans
	private boolean isHeadless;
	private boolean isClient;
	private boolean isMultiplayer;
	private boolean hasAutority;

	private Level activeLevel;
	private GameOptions options;

	// Constructors using custom classes
	public Game(Updater updater, Renderer renderer, Client client, Server server) throws Throwable {
		
		if(client != null && server != null) {
			throw new IllegalArgumentException("Cannot have both a client and a server in a Game");
		}
		
		this.dispatcher = new FunctionDispatcher();
		this.options = new GameOptions();
		this.assetManager = new AssetManager(this);
		this.eventDispatcher = new EventDispatcher();

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
		if(server != null) {
			this.server = server;
			this.server.setName("SERVER");
			registerThread(this.server);
		}

		this.isHeadless = window == null || renderer == null;
		this.isMultiplayer = client != null || server != null;
		this.isClient = (client != null || server == null) || !isMultiplayer;
		this.hasAutority = server != null || !isMultiplayer;
		
		dispatcher.setDefaultId(renderer == null ? -1 : renderer.getId());
	}

	// If the provided game instance dies, this instance does too
	public void depends(Game game) {
		if(!dependencies.contains(game)) {
			dependencies.add(game);
		}
	}
	
	// Reverts the effects of depends()
	public void not_depends(Game game) {
		dependencies.remove(game);
	}
	
	// Stop all child threads and flag this game instance as stopped
	public void stop() {

		if (stopped || stopping || starting) {
			return;
		}
		Logger.info(this, "Waiting for game threads...");
		stopping = true;

		// First stop all threads

		if (updater != null) {
			updater.terminate();
		}
		if (renderer != null) {
			renderer.terminate();
		}
		if (client != null) {
			client.terminate();
		}
		if(server != null) {
			server.terminate();
		}
		
		// Then wait for termination

		if (updater != null) {
			updater.waitTerminate();
		}

		if (renderer != null) {
			renderer.waitTerminate();
		}
		if (client != null) {
			client.waitTerminate();
		}
		if(server != null) {
			server.waitTerminate();
		}
		
		// Destroy the window if it's there

		if (window != null) {
			window.destroy();
		}

		stopped = true;
		stopping = false;

		Logger.info(this, "Stopped");
	}

	public void stopAsync() {
		stopSignal = true;
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

		Logger.loading(this, "Starting game threads...");
		starting = true;
		if(assetManager != null) {
			assetManager.loadAssetSheet(options.getAssetSheetLocation());
		}

		// First start all threads

		if (updater != null) {
			updater.start(this);
		}
		if (renderer != null) {
			renderer.start(this);
		}
		if (client != null) {
			client.start(this);
		}
		if(server != null) {
			server.start(this);
		}

		// Then wait for initialization

		if (updater != null) {
			updater.waitInit();
		}
		if (renderer != null) {
			renderer.waitInit();
		}
		if (client != null) {
			client.waitInit();
		}
		if(server != null) {
			server.waitInit();
		}
		
		// Mark this instance as initialized

		if(updater != null) {
			updater.allowRun();
		}
		if(renderer != null) {
			renderer.allowRun();
		}
		if(client != null) {
			client.allowRun();
		}
		if(server != null) {
			server.allowRun();
		}
		
		init = true;
		starting = false;

		Logger.loading(this, "Ready!");
	}

	// Getters

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
	
	public FunctionDispatcher getFunctionDispatcher() {
		return dispatcher;
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
		if(!serverNull && server.stopped()) {
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
		if (activeLevel != null) {
			return;
		}
		activeLevel = level;
		activeLevel.source = this;
	}

}
