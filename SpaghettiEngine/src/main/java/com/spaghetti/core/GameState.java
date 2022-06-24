package com.spaghetti.core;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

import com.spaghetti.input.Controller;
import com.spaghetti.utils.Utils;

public class GameState {

	// Reflection utility
	private static final Field level_attached = Utils.getPrivateField(Level.class, "attached");

	// Game reference
	protected final Game game;

	// Levels
	protected HashMap<String, Level> levels = new HashMap<>();

	// Players
	protected HashMap<Long, Controller<?>> players = new HashMap<>();

	// Speed
	protected float tickMultiplier = 1;

	public GameState(Game game) {
		this.game = game;
	}

	// Update

	public void update(float delta) {
		for (Level level : levels.values()) {
			// Only active levels
			if (level.isAttached()) {
				level.update(delta);
			}
		}
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

	public void addLevel(String name) {
		if(!levels.containsKey(name)) {
			Level level = new Level(name);
			levels.put(name, level);
		}
	}
	
	public void activateLevel(String name) {
		Level level = levels.get(name);
		level.onBeginPlay();
		Utils.writeField(level_attached, level, true);
	}

	public void activateAllLevels() {
		for(Level level : levels.values()) {
			activateLevel(level.getName());
		}
	}
	
	public void deactivateLevel(String name) {
		Level level = levels.get(name);
		level.onEndPlay();
		Utils.writeField(level_attached, level, false);
	}

	public void deactivateAllLevels() {
		for(Level level : levels.values()) {
			deactivateLevel(level.getName());
		}
	}
	
	public void destroyLevel(String name) {
		Level level = levels.get(name);
		level.destroy();
		levels.remove(name);
	}

	public void destroyAllLevels() {
		for(Level level : levels.values()) {
			destroyLevel(level.getName());
		}
	}
	
	public Level getLevel(String name) {
		return levels.get(name);
	}
	
	// Player management
	
	public void registerPlayer(Long token, Controller<?> player) {
		if(token == null || player == null) {
			return;
		}
		players.put(token, player);
	}
	
	public void unregisterPlayer(Long token) {
		players.remove(token);
	}
	
	public Controller<?> getPlayer(Long token) {
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
		this.tickMultiplier = multiplier;
	}

}
