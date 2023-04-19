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
import com.spaghetti.input.UpdaterComponent;
import com.spaghetti.networking.ClientComponent;
import com.spaghetti.networking.NetworkComponent;
import com.spaghetti.networking.ServerComponent;
import com.spaghetti.render.Camera;
import com.spaghetti.render.RendererComponent;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.GameSettings;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;
import com.spaghetti.world.GameComponent;
import com.spaghetti.world.GameObject;
import com.spaghetti.world.GameState;
import com.spaghetti.world.Level;

public final class Game {

	// Support for multiple instances of the engine
	// in the same java process
	protected static ArrayList<Game> games = new ArrayList<>();
	private static HashMap<Long, Game> links = new HashMap<>();
	private static Object handlerLock = new Object();
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
	private final GameThread primary;
	private final GameThread auxiliary;
	private final ArrayList<GameThread> gameThreads = new ArrayList<>(2);
	private final UpdaterComponent updater;
	private final RendererComponent renderer;
	private final ClientComponent client;
	private final ServerComponent server;
	private final ArrayList<ThreadComponent> threadComponents = new ArrayList<>(4);

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
	public Game(Class<? extends UpdaterComponent> updaterClass, Class<? extends RendererComponent> rendererClass,
				Class<? extends ClientComponent> clientClass, Class<? extends ServerComponent> serverClass,
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

		// Initialize primary thread if an updater or renderer is present
		if(updaterClass != null || rendererClass != null) {
			primary = new GameThread() {
				@Override
				protected GameThread provideSelf() {
					return primary;
				}
			};
		} else {
			primary = null;
		}
		// Initialize the auxiliary thread if a server or client is present
		if(serverClass != null || clientClass != null) {
			auxiliary = new GameThread() {
				@Override
				protected GameThread provideSelf() {
					return auxiliary;
				}
			};
		} else {
			auxiliary = null;
		}

		// Initialize cores
		try {
			if (clientClass != null) {
				client = clientClass.getConstructor().newInstance();
				threadComponents.add(client);
				auxiliary.addComponent(client);
			} else {
				client = null;
			}
			if (serverClass != null) {
				server = serverClass.getConstructor().newInstance();
				threadComponents.add(server);
				auxiliary.addComponent(server);
			} else {
				server = null;
			}
			if (updaterClass != null) {
				updater = updaterClass.getConstructor().newInstance();
				threadComponents.add(updater);
				primary.addComponent(updater);
			} else {
				updater = null;
			}
			if (rendererClass != null) {
				renderer = rendererClass.getConstructor().newInstance();
				threadComponents.add(renderer);
				primary.addComponent(renderer);
			} else {
				renderer = null;
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

		// Register threads
		if (primary != null) {
			gameThreads.add(primary);
			primary.setName("PRIMARY");
			registerThread(primary.getThread());
		}
		if (auxiliary != null) {
			gameThreads.add(auxiliary);
			auxiliary.setName("AUXILIARY");
			registerThread(auxiliary.getThread());
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
		for(GameThread thread : gameThreads) {
			thread.start(this);
		}

		// Then wait for initialization
		Logger.loading(this, "Waiting for initialization...");
		for(GameThread thread : gameThreads) {
			thread.waitInit();
		}

		// Allow threads to run
		Logger.loading(this, "Allowing threads to run...");
		for(GameThread thread : gameThreads) {
			thread.allowRun();
		}

		// Mark this instance as initialized
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
		for(GameThread thread : gameThreads) {
			thread.terminate();
		}

		// Wait for execution to stop
		Logger.info(this, "Waiting for execution to end...");
		for(GameThread thread : gameThreads) {
			thread.waitExecution();
		}

		// Raise shutdown event
		Logger.info(this, "Dispatching stop event...");
		eventDispatcher.raiseEvent(new GameStoppingEvent(this));

		// Allow finalization to begin
		Logger.info(this, "Allowing threads to stop...");
		for(GameThread thread : gameThreads) {
			thread.allowStop();
		}

		// Then wait for finalization
		Logger.info(this, "Executing cleanup...");
		for(GameThread thread : gameThreads) {
			thread.waitTerminate();
		}

		stopped = true;
		stopping = false;

		Logger.info(this, "Stopped");
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

	public GameThread getPrimary() {
		return primary;
	}

	public GameThread getAuxiliary() {
		return auxiliary;
	}

	public RendererComponent getRenderer() {
		return renderer;
	}

	public UpdaterComponent getUpdater() {
		return updater;
	}

	public ClientComponent getClient() {
		return client;
	}

	public ServerComponent getServer() {
		return server;
	}

	public NetworkComponent getNetworkManager() {
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
		for(GameThread thread : gameThreads) {
			if(thread.stopped()) {
				return true;
			}
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

	public long getPrimaryId() {
		return primary.getThread().getId();
	}

	public long getAuxiliaryId() {
		return auxiliary.getThread().getId();
	}

	public FunctionDispatcher getPrimaryDispatcher() {
		return primary.getDispatcher();
	}

	public FunctionDispatcher getAuxiliaryDispatcher() {
		return auxiliary.getDispatcher();
	}

	public int getComponentAmount() {
		return threadComponents.size();
	}

	public ThreadComponent getComponentAt(int index) {
		return threadComponents.get(index);
	}

	public int getThreadAmount() {
		return gameThreads.size();
	}

	public GameThread getThreadAt(int index) {
		return gameThreads.get(index);
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