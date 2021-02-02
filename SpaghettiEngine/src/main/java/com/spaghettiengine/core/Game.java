package com.spaghettiengine.core;

import java.util.*;

import com.spaghettiengine.assets.AssetManager;
import com.spaghettiengine.input.Updater;
import com.spaghettiengine.networking.Client;
import com.spaghettiengine.render.Renderer;
import com.spaghettiengine.utils.Function;
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

	private Updater updater;
	private Renderer renderer;
	private Client client;

	private int index;
	private boolean stopped;
	private boolean init;
	
	protected boolean stopSignal;
	
	// Security
	private boolean starting;
	private boolean stopping;
	
	private Level activeLevel;
	private GameOptions options;

	// Constructors using custom classes
	public Game(Updater updater, Renderer renderer, Client client) throws Throwable {
		this.dispatcher = new FunctionDispatcher();
		this.options = new GameOptions();

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
			assetManager = new AssetManager(this);
		}

		this.dispatcher.setDefaultId(getRendererId());
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

		// Then wait for termination
		
		if (updater != null) {
			updater.waitTerminate();
		}

		if (renderer != null) {
			renderer.waitTerminate();
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

		if(stopped || init || starting || stopping) {
			return;
		}
		
		Logger.loading(this, "Starting game threads...");
		starting = true;
		assetManager.loadAssetSheet(options.getAssetSheetLocation());
		
		// First start all threads

		if (updater != null) {
			updater.start(this);
		}
		if (renderer != null) {
			renderer.start(this);
		}

		// Then wait for initialization

		if (updater != null) {
			updater.waitInit();
		}
		if (renderer != null) {
			renderer.waitInit();
		}

		// Mark this instance as initialized

		updater.allowRun();
		renderer.allowRun();
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
	
	public FunctionDispatcher getFunctionDispatcher() {
		return dispatcher;
	}

	public GameWindow getWindow() {
		return window;
	}

	public boolean isHeadless() {
		return window == null || renderer == null;
	}

	public boolean isDead() {
		boolean updNull = updater == null;
		boolean renderNull = renderer == null;

		if (updNull && renderNull) {
			return true;
		}

		if (!updNull && updater.stopped()) {
			return true;
		}
		if (!renderNull && renderer.stopped()) {
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
