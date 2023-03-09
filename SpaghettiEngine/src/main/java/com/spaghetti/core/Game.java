package com.spaghetti.core;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.events.GameStartedEvent;
import com.spaghetti.core.events.GameStoppingEvent;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.input.Controller;
import com.spaghetti.input.InputDispatcher;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.networking.ClientCore;
import com.spaghetti.networking.NetworkCore;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.render.Camera;
import com.spaghetti.render.RendererCore;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.GameSettings;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;
import com.spaghetti.world.GameObject;
import com.spaghetti.world.GameState;
import com.spaghetti.world.Level;

public final class Game {

	// Support for multiple instances of the engine
	// in the same java process
	protected static ArrayList<Game> games = new ArrayList<>();
	protected static HashMap<Long, Game> links = new HashMap<>();
	protected static Object handlerLock = new Object();
	protected static HandlerThread handlerThread;

	private static void init() {
		synchronized(handlerLock) {
			if (handlerThread == null) {
				HandlerThread newThread = new HandlerThread();
				handlerThread = newThread;
				newThread.start();
				while (newThread.dispatcher == null) {
					ThreadUtil.sleep(1);
				}
			}
		}
	}

	// Stop all instances
	public static void stopAll() {
		for (Game current : games) {
			if (!current.isStopped()) {
				current.stop();
			}
		}

		handlerThread.stop = true;
	}

	// Wait for all instances to finish
	public static void waitAll() {
		while (true) {
			ThreadUtil.sleep(1);
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

	// Static Thread-based methods

	/**
	 * Retrieves the Game instance the current thread is linked to
	 *
	 * @return The Game instance
	 */
	public static Game getInstance() {
		return links.get(Thread.currentThread().getId());
	}

	// Global variables
	private final AssetManager assetManager;
	private final EventDispatcher eventDispatcher;
	private final GameState gameState;
	private final ClientState clientState;
	private final GameSettings options;
	private final InputDispatcher inputDispatcher;
	private final Logger logger;

	// Components
	private final ArrayList<CoreComponent> components = new ArrayList<>(4);
	private final UpdaterCore updater;
	private final RendererCore renderer;
	private final ClientCore client;
	private final ServerCore server;

	// Initialization / Finalization
	private final int index;
	private volatile boolean stopped;
	private volatile boolean init;
	private volatile boolean starting;
	private volatile boolean stopping;

	// Hints for the Handler thread
	protected boolean stopSignal;
	protected ArrayList<Game> dependencies = new ArrayList<>();

	// Cached booleans
	private final boolean isHeadless;
	private final boolean isClient;
	private final boolean isServer;
	private final boolean isMultiplayer;
	private final boolean hasAuthority;

	// Constructors using custom classes
	public Game(Class<? extends UpdaterCore> updaterClass, Class<? extends RendererCore> rendererClass,
			Class<? extends ClientCore> clientClass, Class<? extends ServerCore> serverClass,
			Class<? extends EventDispatcher> eventDispatcherClass, Class<? extends GameSettings> gameOptionsClass,
			Class<? extends AssetManager> assetManagerClass, Class<? extends InputDispatcher> inputDispatcherClass,
			Class<? extends ClientState> clientStateClass, Class<? extends GameState> gameStateClass,
			Class<? extends Logger> loggerClass) {
		// Sanity checks
		if (clientClass != null && serverClass != null) {
			throw new IllegalArgumentException("Cannot have both a client and a server in a Game");
		}
		if (serverClass != null && rendererClass != null) {
			throw new IllegalArgumentException("Cannot have both a server and a renderer in a Game");
		}
		if (updaterClass == null && rendererClass == null) {
			throw new IllegalArgumentException("At least an updater or a renderer is required in a Game");
		}
		if (updaterClass == null && (clientClass != null || serverClass != null)) {
			throw new IllegalArgumentException("Cannot have a client or a server without an updater in a Game");
		}

		// Initialize cores
		try {
			if (clientClass != null) {
				this.client = clientClass.getConstructor().newInstance();
			} else {
				this.client = null;
			}
			if (serverClass != null) {
				this.server = serverClass.getConstructor().newInstance();
			} else {
				this.server = null;
			}
			if (updaterClass != null) {
				this.updater = updaterClass.getConstructor().newInstance();
			} else {
				this.updater = null;
			}
			if (rendererClass != null) {
				this.renderer = rendererClass.getConstructor().newInstance();
			} else {
				this.renderer = null;
			}
		} catch (InstantiationException e) {
			throw new RuntimeException("Error initializing a core: class is abstract", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error initializing a core: exception in constructor", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Error initializing a core: empty constructor is not defined", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error initializing a core: constructor is private", e);
		}

		// Possibly initialize HANDLER and GLFW
		init();

		// Initialize Handler hints
		games.add(this);
		this.index = games.indexOf(this);

		// Register components
		if (updater != null) {
			this.components.add(updater);
			this.updater.setName("UPDATER");
			registerThread(this.updater);
		}
		if (renderer != null) {
			this.components.add(renderer);
			this.renderer.setName("RENDERER");
			registerThread(this.renderer);
		}
		if (client != null) {
			this.components.add(client);
			this.client.setName("CLIENT");
			registerThread(this.client);
		}
		if (server != null) {
			this.components.add(server);
			this.server.setName("SERVER");
			registerThread(this.server);
		}

		// Initialize global variables
		try {
			this.eventDispatcher = eventDispatcherClass.getConstructor(Game.class).newInstance(this);
			this.options = gameOptionsClass.getConstructor(Game.class).newInstance(this);
			this.assetManager = assetManagerClass.getConstructor(Game.class).newInstance(this);
			this.inputDispatcher = inputDispatcherClass.getConstructor(Game.class).newInstance(this);
			this.gameState = gameStateClass.getConstructor(Game.class).newInstance(this);
			this.clientState = clientStateClass.getConstructor(Game.class).newInstance(this);
			this.logger = loggerClass.getConstructor(Game.class).newInstance(this);
		} catch (InstantiationException e) {
			throw new RuntimeException("Error initializing an object: class is abstract", e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Error initializing an object: exception in constructor", e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException("Error initializing an object: please define a constructor taking Game as the only argument", e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Error initializing an object: constructor is private", e);
		}

		// Cache booleans
		this.isHeadless = renderer == null;
		this.isMultiplayer = client != null || server != null;
		this.isClient = (client != null || server == null) || !isMultiplayer;
		this.isServer = (client == null || server != null) && isMultiplayer;
		this.hasAuthority = server != null || !isMultiplayer;
	}

	// If the provided game instance dies, this instance does too
	public void depend(Game game) {
		if (game == null) {
			throw new NullPointerException();
		}
		if (!dependencies.contains(game)) {
			dependencies.add(game);
		}
	}

	// Reverts the effects of depends()
	public void unDepend(Game game) {
		if (game == null) {
			throw new NullPointerException();
		}
		dependencies.remove(game);
	}

	// Linking/Unlinking of threads to this game instance

	public void registerThread(Thread t) {
		long id = t.getId();
		if (links.get(id) == null) {
			links.put(id, this);
		}
	}

	public void registerThread() {
		registerThread(Thread.currentThread());
	}

	public void unregisterThread(Thread t) {
		long id = t.getId();
		if (links.get(id) != null && links.get(id) == this) {
			links.remove(id);
		}
	}

	public void unregisterThread() {
		unregisterThread(Thread.currentThread());
	}

	// Start all child threads
	public void begin() {
		if (stopped || init || starting || stopping) {
			return;
		}

		Logger.loading(this, "Allocating assets...");
		starting = true;

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

		Logger.loading(this, "Waiting for initialization...");
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

		eventDispatcher.raiseEvent(new GameStartedEvent(this));

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

		Logger.info(this, "Dispatching stop event...");
		eventDispatcher.raiseEvent(new GameStoppingEvent(this));

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

		Logger.info(this, "Executing cleanup...");
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

	public RendererCore getRenderer() {
		return renderer;
	}

	public UpdaterCore getUpdater() {
		return updater;
	}

	public ClientCore getClient() {
		return client;
	}

	public ServerCore getServer() {
		return server;
	}

	public NetworkCore getNetworkManager() {
		return client == null ? server : client;
	}

	public GameWindow getWindow() {
		return renderer.getWindow();
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
		return isServer;
	}

	// Returns true if this game is multiplayer
	public boolean isMultiplayer() {
		return isMultiplayer;
	}

	// Returns true if this game is either a server or not multiplayer at all
	public boolean hasAuthority() {
		return hasAuthority;
	}

	public boolean isDead() {
		boolean updNull = updater == null;
		boolean renderNull = renderer == null;
		boolean clientNull = client == null;
		boolean serverNull = server == null;

		if (updNull && renderNull && clientNull && serverNull) {
			return true;
		}

		if ((!updNull && updater.stopped()) || (!renderNull && renderer.stopped())) {
			return true;
		}
		if ((!clientNull && client.stopped()) || (!serverNull && server.stopped())) {
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

	public float getTickMultiplier(float delta) {
		return (delta / 1000f) * gameState.getTickMultiplier();
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

	public FunctionDispatcher getNetworkDispatcher() {
		return getNetworkManager().getDispatcher();
	}

	public int getComponentAmount() {
		return components.size();
	}

	public CoreComponent getComponentAt(int index) {
		return components.get(index);
	}

	public AssetManager getAssetManager() {
		return assetManager;
	}

	public GameSettings getOptions() {
		return options;
	}

	public InputDispatcher getInputDispatcher() {
		return inputDispatcher;
	}

	public Logger getLogger() {
		return logger;
	}

	// Game state

	public GameState getGameState() {
		return gameState;
	}

	public int getLevelsAmount() {
		return gameState.getLevelsAmount();
	}

	public boolean containsLevel(String name) {
		return gameState.containsLevel(name);
	}

	public boolean isLevelActive(String name) {
		return gameState.isLevelActive(name);
	}

	public Level addLevel(String name) {
		return gameState.addLevel(name);
	}

	public void activateLevel(String name) {
		gameState.activateLevel(name);
	}

	public void deactivateLevel(String name) {
		gameState.deactivateLevel(name);
	}

	public void destroyLevel(String name) {
		gameState.destroyLevel(name);
	}

	public Level getLevel(String name) {
		return gameState.getLevel(name);
	}

	public float getTickMultiplier() {
		return gameState.getTickMultiplier();
	}

	public void setTickMultiplier(float multiplier) {
		gameState.setTickMultiplier(multiplier);
	}

	// Client state

	public ClientState getClientState() {
		return clientState;
	}

	public void setLocalCamera(Camera camera) {
		clientState.setLocalCamera(camera);
	}

	public Camera getLocalCamera() {
		return clientState.getLocalCamera();
	}

	public void setLocalPlayer(GameObject player) {
		clientState.setLocalPlayer(player);
	}

	public GameObject getLocalPlayer() {
		return clientState.getLocalPlayer();
	}

	public void setLocalController(Controller<?> controller) {
		clientState.setLocalController(controller);
	}

	public Controller<?> getLocalController() {
		return clientState.getLocalController();
	}

	public void setEngineSetting(String name, Object option) {
		options.setEngineSetting(name, option);
	}

	public Object ngetEngineSetting(String name) {
		return options.ngetEngineSetting(name);
	}

	public <T> T getEngineSetting(String name) {
		return options.<T>getEngineSetting(name);
	}

	public void setSetting(String name, Object option) {
		options.setSetting(name, option);
	}

	public Object ngetSetting(String name) {
		return options.ngetSetting(name);
	}

	public <T> T getSetting(String name) {
		return options.<T>getSetting(name);
	}

}