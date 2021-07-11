package com.spaghetti.networking;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;

public class FieldReplicationRule {

	public static final HashMap<Game, HashMap<Field, FieldReplicationRule>> rules = new HashMap<>();

	protected static enum ReplicationType {
		TOSERVER, TOCLIENT, BIDIRECTIONAL
	}

	protected final Field target;
	protected boolean calculated;
	protected boolean readRule;
	protected boolean writeRule;

	public FieldReplicationRule(Field target) {
		if (target == null) {
			throw new IllegalArgumentException();
		}
		this.target = target;
		synchronized (rules) {
			Game game = Game.getGame();
			HashMap<Field, FieldReplicationRule> game_map = rules.get(game);
			if (game_map == null) {
				game_map = new HashMap<>();
				rules.put(game, game_map);
			}
			game_map.put(target, this);
		}
	}

	protected boolean calculateRule(boolean write) {
		boolean client = Game.getGame().isClient();

		// Allow custom tags on fields only if the enclosing class is Bidirectional
		// otherwise inherit the class' tag
		ReplicationType reptype = ReplicationType.TOCLIENT;
		Class<?> declaring = target.getDeclaringClass();
		if (declaring.getAnnotation(Bidirectional.class) != null) {

			// Determine replication type
			if (target.getAnnotation(ToServer.class) != null) {
				reptype = ReplicationType.TOSERVER;
			} else if (target.getAnnotation(ToClient.class) != null) {
				reptype = ReplicationType.TOCLIENT;
			} else if (target.getAnnotation(Bidirectional.class) != null) {
				reptype = ReplicationType.BIDIRECTIONAL;
			}

		} else {

			// Determine inherited replication type
			if (declaring.getAnnotation(ToServer.class) != null) {
				reptype = ReplicationType.TOSERVER;
			} else if (declaring.getAnnotation(ToClient.class) != null) {
				reptype = ReplicationType.TOCLIENT;
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
			writeRule = calculateRule(true);
			readRule = calculateRule(false);
			calculated = true;
		}
		return writeRule;
	}

	public boolean testRead() {
		if (!calculated) {
			writeRule = calculateRule(true);
			readRule = calculateRule(false);
			calculated = true;
		}
		return readRule;
	}

	// Getters and setters

	public final Field getTargetClass() {
		return target;
	}

}
