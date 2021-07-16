package com.spaghetti.demo;

import org.joml.Vector2i;

import com.spaghetti.core.*;
import com.spaghetti.render.*;

public class SingleplayerDemo {

	public static Game game;

	public static void main(String[] args) {
		// Use a builder
		GameBuilder builder = new GameBuilder();
		builder.setRenderer(RendererCore.class);
		builder.setUpdater(MyUpdater.class);

		game = builder.build();

		// Set custom window size before initializing
		Vector2i resolution = game.getEngineOption("resolution");
		int width = (int) (resolution.x * 0.7f);
		int height = (int) (resolution.y * 0.7f);

		Vector2i size = game.getEngineOption("windowsize");
		size.x = width;
		size.y = height;

		// Initialize
		game.begin();

	}

}
