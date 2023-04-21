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
				resolution.x = 256;
				resolution.y = 256;
			}
		}
		setEngineSetting("openal.enable", true);
		setEngineSetting("resolution", new Vector2i(resolution));
		setEngineSetting("render.resolution", new Vector2i(resolution));
		setEngineSetting("stopTimeout", 10000L); // 10 s
		setEngineSetting("assets.assetSheet", "/res/main.txt");
		setEngineSetting("assets.internalSheet", "/internal/internal_assets.txt");

		// Game window
		setEngineSetting("window.size", new Vector2i(400, 400));
		setEngineSetting("window.minimumSize", new Vector2i(100, 100));
		setEngineSetting("window.maximumSize", new Vector2i(resolution));
		setEngineSetting("window.fullscreen", false);
		setEngineSetting("window.resizable", true);
		setEngineSetting("window.vsync", true);

		setEngineSetting("window.debugContext", true);

		setEngineSetting("window.title", "Spaghetti game");
		setEngineSetting("window.icon16", "/res/icon16.png");
		setEngineSetting("window.icon32", "/res/icon32.png");

		// Networking
		setEngineSetting("network.port", 9018);
		setEngineSetting("network.bufferSize", 1000 * 256); // 256 KB
		setEngineSetting("network.timeoutTime", 500000L);
		setEngineSetting("network.verifyToken", false);
		setEngineSetting("network.maxClients", 10);
		setEngineSetting("network.maxDisconnections", 10);
		setEngineSetting("network.awaitTimeout", 10000L);
		setEngineSetting("network.reconnectAttempts", 10);

		// Logging
		setEngineSetting("log.autoCreate", true);
		setEngineSetting("log.printSeverity", Logger.INFO_SEVERITY);
		setEngineSetting("log.logSeverity", Logger.DEBUG_SEVERITY);

		// Physics
		setEngineSetting("physics.d2.physicsClass", JBox2DPhysics.class);
		setEngineSetting("physics.d2.rigidBodyClass", JBox2DRigidBody.class);
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