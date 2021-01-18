package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.joml.Matrix4d;
import com.spaghettiengine.components.Camera;
import com.spaghettiengine.interfaces.Tickable;

public class Level implements Tickable {

	protected Game source;
	protected ArrayList<GameComponent> components = new ArrayList<>();
	protected HashMap<Long, GameComponent> ordered = new HashMap<>();
	protected Camera activeCamera;

	public Level() {
	}

	public final Game getGame() {
		return source;
	}

	public final void destroy() {
		for (Object obj : components.toArray()) {
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
		if ((found = ordered.get(id)) != null) {
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

	public final void forEachComponent(Consumer<GameComponent> consumer) {
		components.forEach(consumer);
	}

	public final void forEachActualComponent(BiConsumer<Long, GameComponent> consumer) {
		ordered.forEach(consumer);
	}

	private GameComponent _getComponent_GameComponent_return;

	@SuppressWarnings("unchecked")
	public final synchronized <T> T getComponent(Class<T> cls) {
		_getComponent_GameComponent_return = null;
		ordered.forEach((id, component) -> {
			if (component.getClass().equals(cls)) {
				_getComponent_GameComponent_return = component;
			}
		});
		if (_getComponent_GameComponent_return == null) {
			return null;
		}
		return (T) _getComponent_GameComponent_return;
	}

	public final synchronized GameComponent getComponentN(Class<? extends GameComponent> cls) {
		_getComponent_GameComponent_return = null;
		ordered.forEach((id, component) -> {
			if (component.getClass().equals(cls)) {
				_getComponent_GameComponent_return = component;
			}
		});
		return _getComponent_GameComponent_return;
	}

	@Override
	public void update(double delta) {
		components.forEach(component -> {
			component.update(delta);
		});
	}

	public void render(Matrix4d projection, double delta) {
		components.forEach(component -> {
			component.render(projection, delta);
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