package com.spaghettiengine.core;

import java.util.*;

import com.spaghettiengine.utils.FunctionDispatcher;
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
		if(handler != null) handler.stop = true;
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
				return games.get(index);
			}
		}
		return null;
	}

	// Instance globals
	protected FunctionDispatcher dispatcher;
	protected GameWindow window;
	protected AssetManager assetManager;
	
	protected Updater updater;
	protected Renderer renderer;

	protected int index;
	protected boolean stopped;

	protected Level activeLevel;
	protected long tick = 25;

	// Constructors using custom classes
	public Game(Class<? extends Updater> updater, Class<? extends Renderer> renderer) throws Exception {
		this.dispatcher = new FunctionDispatcher();

		games.add(this);
		this.index = games.indexOf(this);

		if (updater != null) {
			this.updater = updater.getConstructor(Game.class).newInstance(this);
			registerThread(this.updater);
		}
		if (renderer != null) {
			this.renderer = renderer.getConstructor(Game.class).newInstance(this);
			registerThread(this.renderer);
			window = new GameWindow(this);
			assetManager = new AssetManager(this);
		}
	}

	// Stop all child threads and flag this game instance as stopped
	public void stop() {
		if (window != null) {
			window.destroy();
		}
		if (updater != null) {
			updater.terminate();
			updater.waitTerminate();
		}
		if (renderer != null) {
			renderer.terminate();
			renderer.waitTerminate();
		}

		stopped = true;
		System.out.println("Stopped " + index);
	}

	public boolean isStopped() {
		return stopped;
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
	public void begin() throws Exception {
		// First start all threads
		if (updater != null) {
			updater.start();
		}
		if (renderer != null) {
			renderer.start();
		}
		
		// Then wait for initialization
		
		if(updater != null) {
			updater.waitInit();
		}
		if(renderer != null) {
			renderer.waitInit();
		}
		
		System.out.println("Started " + index);
	}

	// Getters

	public Renderer getRenderer() {
		return renderer;
	}

	public Updater getUpdater() {
		return updater;
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

	public int getIndex() {
		return index;
	}

	public long getTick() {
		return tick;
	}

	public void setTick(long tick) {
		this.tick = tick;
	}

	public float getTickMultiplier(float delta) {
		return delta / tick;
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
	
	public void detachLevel() {
		if (activeLevel == null) {
			return;
		}
		activeLevel.source = null;
		activeLevel = null;
	}
	
	public void attachLevel(Level level) {
		if(activeLevel != null) {
			return;
		}
		activeLevel = level;
		activeLevel.source = this;
	}
	
}
