package com.spaghetti.utils;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.HashMap;

import com.spaghetti.physics.d2.jbox2d.JBox2DPhysics;
import com.spaghetti.physics.d2.jbox2d.JBox2DRigidBody;
import org.joml.Vector2i;

import com.spaghetti.core.Game;

public class GameSettings {

	protected final Game game;

	public static final String PREFIX = "com.spaghetti.";
	protected final HashMap<String, Object> settings = new HashMap<>();

	public GameSettings(Game game) {
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
		setEngineSetting("openal.enable", true);
		setEngineSetting("resolution", new Vector2i(resolution));
		setEngineSetting("renderresolution", new Vector2i(resolution));
		setEngineSetting("stoptimeout", 10000l); // 10 s
		setEngineSetting("assetsheet", "/res/main.txt");
		setEngineSetting("assets.internalSheet", "/internal/internal_assets.txt");

		// Game window
		setEngineSetting("windowsize", new Vector2i(400, 400));
		setEngineSetting("windowminimumsize", new Vector2i(100, 100));
		setEngineSetting("windowmaximumsize", new Vector2i(resolution));
		setEngineSetting("windowfullscreen", false);
		setEngineSetting("windowresizable", true);

		setEngineSetting("debugcontext", true);

		setEngineSetting("windowtitle", "Spaghetti game");
		setEngineSetting("windowicon16", "/res/icon16.png");
		setEngineSetting("windowicon32", "/res/icon32.png");

		// Networking
		setEngineSetting("networkport", 9018);
		setEngineSetting("networkbuffer", 1000 * 256); // 256 KB
		setEngineSetting("networktimeout", 500000l);
		setEngineSetting("networkverifytoken", false);
		setEngineSetting("networkmaxclients", 10);
		setEngineSetting("networkmaxdisconnections", 10);
		setEngineSetting("networkawaittimeout", 10000l);
		setEngineSetting("networkreconnectattempts", 10);

		// Logging
		setEngineSetting("logautocreate", true);
		setEngineSetting("logseverityprint", 1);
		setEngineSetting("logseveritylog", 0);

		// Physics
		setEngineSetting("physics.d2.physicsClass", JBox2DPhysics.class.getName());
		setEngineSetting("physics.d2.rigidBodyClass", JBox2DRigidBody.class.getName());
	}

	// Public getters and setters

	public static final void ssetEngineSetting(String name, Object value) {
		Game.getInstance().getOptions().setEngineSetting(name, value);
	}

	public static final Object sngetEngineSetting(String name) {
		return Game.getInstance().getOptions().ngetEngineSetting(name);
	}

	public static final <T> T sgetEngineSetting(String name) {
		return Game.getInstance().getOptions().<T>getEngineSetting(name);
	}

	public static final void ssetSetting(String name, Object value) {
		Game.getInstance().getOptions().setSetting(name, value);
	}

	public static final Object sngetSetting(String name) {
		return Game.getInstance().getOptions().ngetSetting(name);
	}

	public static final <T> T sgetSetting(String name) {
		return Game.getInstance().getOptions().<T>getSetting(name);
	}

	public final void setEngineSetting(String name, Object value) {
		setSetting(PREFIX + name, value);
	}

	public final Object ngetEngineSetting(String name) {
		return settings.get(PREFIX + name);
	}

	@SuppressWarnings("unchecked")
	public final <T> T getEngineSetting(String name) {
		return (T) settings.get(PREFIX + name);
	}

	public void setSetting(String name, Object value) {
		// If the game is not initialized, the event dispatcher
		// won't be available
		if(!game.isInit()) {
			settings.put(name, value);
			return;
		}

		// Otherwise send an event and let the listeners change
		// the new value
		if(settings.containsKey(name)) {
			Object oldValue = settings.get(name);
			SettingChangedEvent event = new SettingChangedEvent(name, oldValue, value);
			game.getEventDispatcher().raiseEvent(event);

			if(!event.isCancelled()) {
				settings.put(event.getSettingName(), event.getNewValue());
			}
		}
	}

	public final Object ngetSetting(String name) {
		return settings.get(name);
	}

	@SuppressWarnings("unchecked")
	public final <T> T getSetting(String name) {
		return (T) settings.get(name);
	}

}