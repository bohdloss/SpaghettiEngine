package com.spaghetti.networking;

import java.util.HashMap;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;

public class ClassReplicationRule {

	public static final HashMap<Game, HashMap<Class<?>, ClassReplicationRule>> rules = new HashMap<>();

	protected static enum ReplicationType {
		TOSERVER, TOCLIENT, BIDIRECTIONAL
	}

	protected final Class<?> target;
	protected boolean calculated;
	protected boolean readRule;
	protected boolean writeRule;
	protected long lastCheck;
	protected long delay;

	public ClassReplicationRule(Class<?> target) {
		if (target == null) {
			throw new IllegalArgumentException();
		}
		this.target = target;
		synchronized (rules) {
			Game game = Game.getGame();
			HashMap<Class<?>, ClassReplicationRule> game_map = rules.get(game);
			if (game_map == null) {
				game_map = new HashMap<>();
				rules.put(game, game_map);
			}
			game_map.put(target, this);
		}
	}

	protected boolean calculateRule(boolean write) {
		boolean client = Game.getGame().isClient();
		if (target.getAnnotation(NoReplicate.class) != null) {
			return false;
		}

		// Determine replication type
		ReplicationType reptype = ReplicationType.TOCLIENT;
		if (target.getAnnotation(ToServer.class) != null) {
			reptype = ReplicationType.TOSERVER;
		} else if (target.getAnnotation(ToClient.class) != null) {
			reptype = ReplicationType.TOCLIENT;
		} else if (target.getAnnotation(Bidirectional.class) != null) {
			reptype = ReplicationType.BIDIRECTIONAL;
		}

		if (target.getAnnotation(ReplicationDelay.class) != null) {
			ReplicationDelay da = target.getAnnotation(ReplicationDelay.class);
			if (client) {
				delay = da.toServer();
			} else {
				delay = da.toClient();
			}
		}

		switch (reptype) {
		case TOCLIENT:
			return client != write;
		case TOSERVER:
			return client == write;
		case BIDIRECTIONAL:
			return true;
		}
		return true;
	}

	public boolean testWrite() {
		if (!calculated) {
			doInit();
		}
		return writeRule && (System.currentTimeMillis() > lastCheck + delay);
	}

	public boolean testRead() {
		if (!calculated) {
			doInit();
		}
		return readRule;
	}

	protected void doInit() {
		writeRule = calculateRule(true);
		readRule = calculateRule(false);
		calculated = true;
	}

	// Getters and setters

	public final Class<?> getTargetClass() {
		return target;
	}

}
