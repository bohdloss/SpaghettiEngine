package com.spaghetti.demo;

import com.spaghetti.world.GameState;
import org.joml.Vector2i;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameBuilder;

public class SingleplayerDemo {

	public static Game game;

	public static void main(String[] args) {
		Game.initialize();

		// Use a game builder
		GameBuilder builder = new GameBuilder();
		builder.enableRenderer();
		builder.enableUpdater();
		game = builder.build();

		// Set custom window size before initializing
		Vector2i resolution = game.getEngineSetting("resolution");
		int width = (int) (resolution.x * 0.7f);
		int height = (int) (resolution.y * 0.7f);

		game.setEngineSetting("window.fullscreen", false);

		Vector2i size = game.getEngineSetting("window.size");
		size.x = width;
		size.y = height;

		// Add F11 listener that toggles fullscreen
		game.getInputDispatcher().registerListener(new FullscreenListener());

		// Set custom game mode
		game.getGameState().setGameMode(new DemoMode());

		// Initialize
		game.beginAsync();

		Game.idle();
	}

}
