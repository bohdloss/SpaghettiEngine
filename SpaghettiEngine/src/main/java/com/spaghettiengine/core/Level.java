package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.joml.Vector2d;

import com.spaghettiengine.render.*;

public class Level implements Tickable {

	protected Game source;
	protected ArrayList<GameComponent> components = new ArrayList<>();
	protected HashMap<Long, GameComponent> ordered = new HashMap<>();
	protected Camera activeCamera;

	// Physics related

	protected double killY = -10000;
	protected Vector2d gravity = new Vector2d();

	public Level(Game source) {
		this.source = source;
	}

	protected final void addComponent(GameComponent component) {
		components.add(component);
		ordered.put(component.getId(), component);
	}

	public final void removeComponent(long id) {
		GameComponent removed = ordered.remove(id);
		if (removed != null) {
			components.remove(removed);
		}
	}

	@Override
	public void update(float delta) {
		components.forEach(component -> {
			component.update(delta);
		});
	}

}
