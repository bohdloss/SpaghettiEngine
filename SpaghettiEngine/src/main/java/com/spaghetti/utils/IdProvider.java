package com.spaghetti.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import com.spaghetti.core.Game;

public final class IdProvider {

	private static final HashMap<Game, ArrayList<Integer>> games = new HashMap<>();
	private static final Random random = new Random();

	private IdProvider() {
	}

	public static int newId(Game instance) {
		// Retrieve existing ids
		ArrayList<Integer> ids = games.get(instance);
		if (ids == null) {
			ids = new ArrayList<>();
			games.put(instance, ids);
		}

		// Generate new one until it stops colliding with others
		int res = 0;
		while (res == 0 || ids.contains(res)) {
			res = genId();
		}

		return res;
	}

	public static void freeId(Game instance, int id) {
		// Retrieve existing ids
		ArrayList<Integer> ids = games.get(instance);
		if(ids != null) {
			ids.remove(id);
		}
	}

	private static int genId() {
		// Reserve -1, 0 and 1
		int rand = 0;
		while (rand == 0 || rand == -1 || rand == 1) {
			rand = random.nextInt();
		}
		return rand;
	}

}
