package com.spaghetti.world;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.spaghetti.core.EmptyMode;
import com.spaghetti.core.Game;
import com.spaghetti.exceptions.GameStateException;
import com.spaghetti.input.Controller;
import com.spaghetti.input.Updatable;
import com.spaghetti.networking.Replicable;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.ExceptionUtil;
import com.spaghetti.utils.ReflectionUtil;
import com.spaghetti.utils.ThreadUtil;

/**
 * GameState contains data that is owned by the server
 * but is shared among clients.
 * <p>
 * The server may omit or obfuscate the data from this class
 * as long as the necessary information, such as
 * the game mode, info about the player owned by the client
 * and the level it is in, is replicated
 *
 *
 * @author bohdloss
 *
 */
public class GameState implements Updatable, Replicable {

	// Reflection utility
	private static final Field level_attached = ReflectionUtil.getPrivateField(Level.class, "attached");

	// Game reference
	protected final Game game;
	protected GameMode gameMode;

	// Levels
	protected HashMap<String, Level> levels = new HashMap<>();

	// Players
	protected HashMap<Long, Controller<?>> players = new HashMap<>();

	// Speed
	protected float tickMultiplier = 1;

	// Something changed?
	protected boolean replication;

	public GameState(Game game) {
		this.game = game;
		this.gameMode = new EmptyMode(this);
	}

	// Update

	@Override
	public void update(float delta) {
		gameMode.initialize();
		gameMode.update(delta);

		for (Level level : levels.values()) {
			// Only active levels
			if (level.isAttached()) {
				level.update(delta);
			}
		}
	}

	public void destroy() {
		if(gameMode != null) {
			gameMode.destroy();
		}
		unregisterAllPlayers();
		destroyAllLevels();
	}

	// Level management

	public int getLevelsAmount() {
		return levels.size();
	}

	public boolean containsLevel(String name) {
		return levels.containsKey(name);
	}

	public boolean isLevelActive(String name) {
		return levels.get(name).isAttached();
	}

	public Level addLevel(String name) {
		if(name == null) {
			throw new NullPointerException();
		}
		if(!levels.containsKey(name)) {
			Level level = new Level(game, name);
			levels.put(name, level);
			replication = true; // Change detected
			return level;
		}
		throw new GameStateException("Level with the same name is already registered: " + name);
	}

	public void activateLevel(String name) {
		activateLevel(levels.get(name));
	}

	public void activateLevel(Level level) {
		if(level == null) {
			throw new NullPointerException();
		}
		if(!levels.containsValue(level)) {
			throw new GameStateException("Cannot activate unknown level " + level.getName());
		}
		level.onBeginPlay();
		ReflectionUtil.writeField(level_attached, level, true);
		replication = true; // Change detected
	}

	public void activateAllLevels() {
		for(Level level : levels.values()) {
			activateLevel(level.getName());
		}
	}

	public void deactivateLevel(String name) {
		deactivateLevel(levels.get(name));
	}

	public void deactivateLevel(Level level) {
		if(level == null) {
			throw new NullPointerException();
		}
		if(!levels.containsValue(level)) {
			throw new GameStateException("Cannot deactivate unknown level " + level.getName());
		}
		level.onEndPlay();
		ReflectionUtil.writeField(level_attached, level, false);
		replication = true; // Change detected
	}

	public void deactivateAllLevels() {
		for(Level level : levels.values()) {
			deactivateLevel(level.getName());
		}
	}

	public void destroyLevel(String name) {
		destroyLevel(levels.get(name));
	}

	public void destroyLevel(Level level) {
		if(level == null) {
			throw new NullPointerException();
		}
		if(!levels.containsValue(level)) {
			throw new GameStateException("Cannot destroy unregistered level " + level.getName());
		}
		String name = level.getName();
		level.destroy();
		levels.remove(name);
	}

	public void destroyAllLevels() {
		for(Level level : levels.values()) {
			destroyLevel(level.getName());
		}
	}

	public Level getLevel(String name) {
		if(name == null) {
			throw new NullPointerException();
		}
		return levels.get(name);
	}

	// Game mode management

	public void setGameMode(GameMode gameMode) {
		if(gameMode == null) {
			throw new NullPointerException();
		}
		if(gameMode.gameState != this) {
			throw new IllegalArgumentException();
		}
		if(this.gameMode != null) {
			this.gameMode.destroy();
		}
		this.gameMode = gameMode;
	}

	public GameMode getGameMode() {
		return gameMode;
	}

	// Player management

	public void registerPlayer(Long token, Controller<?> player) {
		if(token == null || player == null) {
			throw new NullPointerException();
		}
		players.put(token, player);
	}

	public void unregisterPlayer(Long token) {
		if(token == null) {
			throw new NullPointerException();
		}
		players.remove(token);
	}

	public Controller<?> getPlayer(Long token) {
		if(token == null) {
			throw new NullPointerException();
		}
		return players.get(token);
	}

	public void unregisterAllPlayers() {
		players.clear();
	}

	// Speed

	public float getTickMultiplier() {
		return tickMultiplier;
	}

	public void setTickMultiplier(float multiplier) {
		if(multiplier <= 0) {
			throw new GameStateException("Tick multiplier must be greater than 0");
		}
		this.tickMultiplier = multiplier;
	}

	// Generic getters

	public final Game getGame() {
		return game;
	}

	// Client server interface

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer dataBuffer) {
		
	}

	@Override
	public void readDataClient(ConnectionManager manager, NetworkBuffer dataBuffer) {
		
	}

	@Override
	public boolean needsReplication(ConnectionManager connection) {
		boolean previous = replication;
		replication = false;
		return previous;
	}

	@Override
	public boolean isLocal() {
		return false;
	}

}
