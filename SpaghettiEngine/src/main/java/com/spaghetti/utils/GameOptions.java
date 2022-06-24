package com.spaghetti.utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;

import org.joml.Vector2i;

import com.spaghetti.core.Game;

public class GameOptions {

	protected final Game game;

	public static final String PREFIX = "com.spaghetti.";
	protected final HashMap<String, Object> options = new HashMap<>();

	public GameOptions(Game game) {
		this.game = game;

		// Find the resolution
		Vector2i resolution = new Vector2i();
		if (!"true".equalsIgnoreCase(System.getProperty("java.awt.headless"))) {
			try {
				Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
				resolution.x = dimension.width;
				resolution.y = dimension.height;
			} catch (Throwable t) {
				resolution.x = 1;
				resolution.y = 1;
			}
		}
		setEngineOption("resolution", new Vector2i(resolution));
		setEngineOption("renderresolution", new Vector2i(resolution));
		setEngineOption("stoptimeout", 10000l); // 10 s
		setEngineOption("assetsheet", "/res/main.txt");

		// Game window
		setEngineOption("windowsize", new Vector2i(400, 400));
		setEngineOption("windowminimumsize", new Vector2i(100, 100));
		setEngineOption("windowmaximumsize", new Vector2i(resolution));
		setEngineOption("windowfullscreen", false);
		setEngineOption("windowresizable", true);

		setEngineOption("debugcontext", true);

		setEngineOption("windowtitle", "Spaghetti game");
		setEngineOption("windowicon16", "/res/icon16.png");
		setEngineOption("windowicon32", "/res/icon32.png");

		// Networking
		setEngineOption("networkport", 9018);
		setEngineOption("networkbuffer", 1000 * 256); // 256 KB
		setEngineOption("networktimeout", 500000l);
		setEngineOption("networkverifytoken", false);
		setEngineOption("networkmaxclients", 10);
		setEngineOption("networkmaxdisconnections", 10);
		setEngineOption("networkawaittimeout", 10000l);
		setEngineOption("networkreconnectattempts", 10);
	}

	// Public getters and setters

	public static void ssetEngineOption(String name, Object value) {
		Game.getGame().getOptions().setEngineOption(name, value);
	}

	public static Object sngetEngineOption(String name) {
		return Game.getGame().getOptions().ngetEngineOption(name);
	}

	public static <T> T sgetEngineOption(String name) {
		return Game.getGame().getOptions().<T>getEngineOption(name);
	}

	public static void ssetOption(String name, Object value) {
		Game.getGame().getOptions().setOption(name, value);
	}

	public static Object sngetOption(String name) {
		return Game.getGame().getOptions().ngetOption(name);
	}

	public static <T> T sgetOption(String name) {
		return Game.getGame().getOptions().<T>getOption(name);
	}

	public void setEngineOption(String name, Object value) {
		options.put(PREFIX + name, value);
	}

	public Object ngetEngineOption(String name) {
		return options.get(PREFIX + name);
	}

	@SuppressWarnings("unchecked")
	public <T> T getEngineOption(String name) {
		return (T) options.get(PREFIX + name);
	}

	public void setOption(String name, Object value) {
		options.put(name, value);
	}

	public Object ngetOption(String name) {
		return options.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T getOption(String name) {
		return (T) options.get(name);
	}

}