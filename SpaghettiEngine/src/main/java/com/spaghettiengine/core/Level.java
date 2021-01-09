package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.joml.Matrix4d;
import org.joml.Vector2d;

import com.spaghettiengine.components.Camera;
import com.spaghettiengine.interfaces.Tickable;

public class Level implements Tickable {

	protected Game source;
	protected ArrayList<GameComponent> components = new ArrayList<>();
	protected HashMap<Long, GameComponent> ordered = new HashMap<>();
	protected Camera activeCamera;

	// Physics related
	protected Vector2d gravity = new Vector2d();

	public Level() {
	}

	public Game getGame() {
		return source;
	}
	
	public void destroy() {
		for(Object obj : components.toArray()) {
			GameComponent gc = (GameComponent) obj;
			gc.destroy();
		}
	}
	
	protected synchronized final void addComponent(GameComponent component) {
		components.add(component);
		ordered.put(component.getId(), component);
	}

	public synchronized final void removeComponent(long id) {
		GameComponent removed = ordered.remove(id);
		if (removed != null) {
			components.remove(removed);
		}
	}

	public synchronized final void deleteComponent(long id) {
		GameComponent found;
		if((found = ordered.get(id)) != null) {
			found.destroy();
		}
	}

	public final GameComponent getComponent(long id) {
		return ordered.get(id);
	}

	public final int getComponentAmount() {
		return components.size();
	}

	public final int getActualComponentAmount() {
		return ordered.size();
	}

	@Override
	public void update(double delta) {
		components.forEach(component -> {
			component.update(delta);
		});
	}

	public void render(Matrix4d projection) {
		components.forEach(component -> {
			component.render(projection);
		});
	}

	public Camera getActiveCamera() {
		return activeCamera;
	}

	public void detachCamera() {
		if (activeCamera == null) {
			return;
		}
		activeCamera = null;
	}

	public void attachCamera(Camera camera) {
		if (activeCamera != null || camera.getLevel() != this) {
			camera.calcScale();
			return;
		}
		activeCamera = camera;
	}

}