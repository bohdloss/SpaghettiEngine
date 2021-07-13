package com.spaghetti.core;

import java.util.*;

import com.spaghetti.assets.AssetManager;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.Signals;
import com.spaghetti.input.Controller;
import com.spaghetti.input.InputDispatcher;
import com.spaghetti.input.UpdaterCore;
import com.spaghetti.networking.ClientCore;
import com.spaghetti.networking.ServerCore;
import com.spaghetti.objects.Camera;
import com.spaghetti.render.RendererCore;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.GameOptions;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public final class Game {

	// Support for multiple instances of the engine
	// in the same java process
	protected volatile static ArrayList<Game> games = new ArrayList<>();
	protected volatile static HashMap<Long, Game> links = new HashMap<>();
	protected volatile static Handler handler;

	private static void init() {
		if (handler == null) {
			handler = new Handler();
			handler.start();
			while (handler.dispatcher == null) {
				Utils.sleep(1);
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

	// Static Thread-based methods

	public static Game getGame() {
		return links.get(Thread.currentThread().getId());
	}

	// Global variables
	private volatile AssetManager assetManager;
	private volatile EventDispatcher eventDispatcher;
	private volatile ClientState clientState;
	private volatile GameOptions options;
	private volatile InputDispatcher inputDispatcher;

	// Components
	private final ArrayList<CoreComponent> components = new ArrayList<>(4);
	private volatile UpdaterCore updater;
	private volatile RendererCore renderer;
	private volatile ClientCore client;
	private volatile ServerCore server;

	// Initialization / Finalization
	private volatile int index;
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
	private final boolean hasAutority;

	// Constructors using custom classes
	public Game(UpdaterCore updater, RendererCore renderer, ClientCore client, ServerCore server,
			Class<? extends EventDispatcher> eventDispatcherClass, Class<? extends GameOptions> gameOptionsClass,
			Class<? extends AssetManager> assetManagerClass, Class<? extends InputDispatcher> inputDispatcherClass,
			Class<? extends ClientState> clientStateClass) throws Throwable {
		// Sanity checks
		if (client != null && server != null) {
			throw new IllegalArgumentException("Cannot have both a client and a server in a Game");
		}
		if (server != null && renderer != null) {
			throw new IllegalArgumentException("Cannot have both a server and a renderer in a Game");
		}
		if (updater == null && renderer == null) {
			throw new IllegalArgumentException("At least an updater or a renderer is required in a Game");
		}
		if (updater == null && (client != null || server != null)) {
			throw new IllegalArgumentException("Cannot have a client or a server without an updater in a Game");
		}

		// Possibly initialize HANDLER and GLFW
		init();

		// Initialize Handler hints
		games.add(this);
		this.index = games.indexOf(this);

		// Register components
		if (updater != null) {
			this.updater = updater;
			this.components.add(updater);
			this.updater.setName("UPDATER");
			registerThread(this.updater);
		}
		if (renderer != null) {
			this.renderer = renderer;
			this.components.add(renderer);
			this.renderer.setName("RENDERER");
			registerThread(this.renderer);
		}
		if (client != null) {
			this.client = client;
			this.components.add(client);
			this.client.setName("CLIENT");
			registerThread(this.client);
		}
		if (server != null) {
			this.server = server;
			this.components.add(server);
			this.server.setName("SERVER");
			registerThread(this.server);
		}

		// Initialize global variables
		this.eventDispatcher = eventDispatcherClass.getConstructor(Game.class).newInstance(this);
		this.options = gameOptionsClass.getConstructor(Game.class).newInstance(this);
		this.assetManager = assetManagerClass.getConstructor(Game.class).newInstance(this);
		this.inputDispatcher = inputDispatcherClass.getConstructor(Game.class).newInstance(this);
		this.clientState = clientStateClass.getConstructor(Game.class).newInstance(this);

		// Cache booleans
		this.isHeadless = renderer == null;
		this.isMultiplayer = client != null || server != null;
		this.isClient = (client != null || server == null) || !isMultiplayer;
		this.isServer = (client == null || server != null) && isMultiplayer;
		this.hasAutority = server != null || !isMultiplayer;
	}

	// If the provided game instance dies, this instance does too
	public void depends(Game game) {
		if (game == null) {
			throw new NullPointerException();
		}
		if (!dependencies.contains(game)) {
			dependencies.add(game);
		}
	}

	// Reverts the effects of depends()
	public void not_depends(Game game) {
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
	public void begin() throws Throwable {
		if (stopped || init || starting || stopping) {
			return;
		}

		Logger.loading(this, "Allocating assets...");
		starting = true;
		// Allocate asset manager
		if (assetManager != null) {
			assetManager.loadAssetSheet(options.getEngineOption("assetsheet"));
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

		Logger.info(this, "Dispatching stop signal...");
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
		return (delta / 1000) * clientState.getTickMultiplier();
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

	public int getComponentAmount() {
		return components.size();
	}

	public CoreComponent getComponentAt(int index) {
		return components.get(index);
	}

	public AssetManager getAssetManager() {
		return assetManager;
	}

	public GameOptions getOptions() {
		return options;
	}

	public InputDispatcher getInputDispatcher() {
		return inputDispatcher;
	}

	// Client state

	public ClientState getClientState() {
		return clientState;
	}

	public Level getActiveLevel() {
		return clientState.getActiveLevel();
	}

	public void attachLevel(Level level) {
		clientState.attachLevel(level);
	}

	public void detachLevel() {
		clientState.detachLevel();
	}

	public Camera getActiveCamera() {
		return clientState.getActiveCamera();
	}

	public void attachCamera(Camera camera) {
		clientState.attachCamera(camera);
	}

	public void detachCamera() {
		clientState.detachCamera();
	}

	public Controller getActiveController() {
		return clientState.getActiveController();
	}

	public void attachController(Controller controller) {
		clientState.attachController(controller);
	}

	public void detachController() {
		clientState.detachController();
	}

	public void setEngineOption(String name, Object option) {
		options.setEngineOption(name, option);
	}

	public Object ngetEngineOption(String name) {
		return options.ngetEngineOption(name);
	}

	public <T> T getEngineOption(String name) {
		return options.<T>getEngineOption(name);
	}

	public void setOption(String name, Object option) {
		options.setOption(name, option);
	}

	public Object ngetOption(String name) {
		return options.ngetOption(name);
	}

	public <T> T getOption(String name) {
		return options.<T>getOption(name);
	}

}