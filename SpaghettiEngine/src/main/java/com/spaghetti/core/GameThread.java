package com.spaghetti.core;

import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class GameThread {

	private Thread thread;
	private boolean isMain;
	private volatile Game game;
	private FunctionDispatcher functionDispatcher;
	private volatile boolean stop;
	private volatile boolean init;
	private volatile boolean allowRun;
	private volatile boolean allowStop;
	private volatile boolean executionEnd;
	private volatile boolean requestChance;
	private volatile long lastTime;
	private volatile boolean run;
	private String name;

	private List<ThreadComponent> componentList = new ArrayList<>(4);

	public final void initializeThread(Thread thread) {
		if(this.thread != null) {
			return;
		}
		if(thread == null) {
			thread = new Thread(() -> run());
		} else {
			isMain = true;
		}
		this.thread = thread;
		this.thread.setName(name);
		functionDispatcher = new FunctionDispatcher(thread);
	}

	public final void initialize() throws Throwable {
		if (!validStarting()) {
			throw new IllegalStateException(
					"Error: attempted to initialize core thread state outside the context of a game");
		}
		try {
			for(ThreadComponent component : componentList) {
				Logger.info("Initializing " + component.getClass().getSimpleName() + "...");
				component.initialize(getGame());
			}
		} finally {
			init = true;
		}
	}

	public final void terminate() {
		if (!validStopping()) {
			throw new IllegalStateException("Error: attempted to stop core thread outside the context of a game");
		}
		stop = true;
	}

	public final void waitInit() {
		if(isMain) {
			runInit();
		} else {
			while (!initialized()) {
				ThreadUtil.sleep(1);
			}
		}
	}

	public final void waitTerminate() {
		while (!stopped()) {
			ThreadUtil.sleep(1);
		}
	}

	public final void waitExecution() {
		while (!executionEnd) {
			ThreadUtil.sleep(1);
		}
	}

	public final void allowRun() {
		if (!validStarting()) {
			throw new IllegalStateException(
					"Error: attempted to modify core thread state outside the context of a game");
		}
		allowRun = true;
	}

	public final void allowRunDelayed() {
		if(isMain) {
			runLoop();
		}
	}

	public final void allowStop() {
		if (!validStopping()) {
			throw new IllegalStateException(
					"Error: attempted to modify core thread state outside the context of a game");
		}
		allowStop = true;
	}


	public final void start() {
		if (!validStarting()) {
			throw new IllegalStateException("Error: attempted to start core thread outside the context of a game");
		}
		if(!isMain) {
			thread.start();
		}
	}

	public final void start(Game game) {
		if (this.game != null && this.game != game) {
			throw new IllegalStateException("Error: attempted to start core multiple times in different games");
		}
		this.game = game;
		start();
	}


	public final void run() {
		runInit();
		runLoop();
	}

	private void runInit() {
		run = true;
		if (Thread.currentThread().getId() != thread.getId()) {
			throw new IllegalStateException("Error: run() called but no new thread started");
		}

		// Initialization code is ran here
		try {
			initialize();
		} catch (Throwable t) {
			_uncaught(t);
			stop = true;
		}
	}

	private void runLoop() {
		// Game loop
		try {
			while (!allowRun) {
				functionDispatcher.computeEvents();
				ThreadUtil.sleep(1);
			}
			for (ThreadComponent component : componentList) {
				component.postInitialize();
			}
			while (!stop) {
				// Calculate delta
				long current = System.currentTimeMillis();
				if (lastTime == 0) {
					lastTime = current;
				}
				long pre = current - lastTime;
				float delta = pre;
				lastTime = current;

				// Compute queued operations
				functionDispatcher.computeEvents();
				if(isMain) {
					Game.loop();
				}
				for (ThreadComponent component : componentList) {
					component.loop(game.getTickMultiplier(delta));
				}
			}
			for (ThreadComponent component : componentList) {
				component.preTerminate();
			}
		} catch (Throwable t) {
			_uncaught(t);
		} finally {
			stop = true;
			game.stopAsync();
		}

		executionEnd = true;
		while (!allowStop) {
			ThreadUtil.sleep(1);
			functionDispatcher.computeEvents();
		}

		// Terminate
		try {
			for(ThreadComponent component : componentList) {
				component.terminate();
			}
		} catch(Throwable t) {
			_uncaught(t);
		}
		if(isMain) {
			Game.terminate();
		}

		// Final chance to handle some requests
		requestChance = true;
		while (true) {
			boolean found = true;
			for (int i = 0; i < game.getThreadAmount(); i++) {
				if (!game.getThreadAt(i).requestChance) {
					found = false;
					break;
				}
			}
			if (found) {
				break;
			}
			ThreadUtil.sleep(1);
			functionDispatcher.computeEvents();
		}
	}

	private void _uncaught(Throwable t) {
		Logger.error("Fatal uncaught error in game " + game.getIndex() + ":", t);
	}

	public void addComponent(ThreadComponent component) {
		if(!run && !componentList.contains(component)) {
			componentList.add(component);
		}
	}

	// Getters

	public final boolean stopped() {
		return stop && !thread.isAlive();
	}

	public final boolean initialized() {
		return init;
	}

	public final boolean executionEnded() {
		return executionEnd;
	}

	protected abstract GameThread provideSelf();

	private final boolean validStarting() {
		return game != null && game.isStarting() && this == provideSelf();
	}

	private final boolean validStopping() {
		return game != null && game.isStopping() && this == provideSelf();
	}

	// Getters and setters

	public final Game getGame() {
		return game;
	}

	public final FunctionDispatcher getDispatcher() {
		return functionDispatcher;
	}

	public final void setName(String name) {
		if(thread == null) {
			this.name = name;
		} else {
			this.thread.setName(name);
		}
	}

    public Thread getThread() {
        return thread;
    }

}
