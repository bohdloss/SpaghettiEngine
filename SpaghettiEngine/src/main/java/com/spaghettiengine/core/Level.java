package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.joml.Matrix4d;

import com.spaghettiengine.interfaces.Tickable;
import com.spaghettiengine.objects.Camera;

public class Level implements Tickable {

	protected Game source;
	protected ArrayList<GameObject> objects = new ArrayList<>();
	protected HashMap<Long, GameObject> ordered = new HashMap<>();
	protected Camera activeCamera;

	public Level() {
	}

	public final Game getGame() {
		return source;
	}

	public final void destroy() {
		for (Object obj : objects.toArray()) {
			GameObject go = (GameObject) obj;
			go.destroy();
		}
	}

	protected synchronized final void addObject(GameObject component) {
		objects.add(component);
		ordered.put(component.getId(), component);
	}

	public synchronized final void removeObject(long id) {
		GameObject removed = ordered.remove(id);
		if (removed != null) {
			objects.remove(removed);
		}
	}

	public synchronized final void deleteObject(long id) {
		GameObject found;
		if ((found = ordered.get(id)) != null) {
			found.destroy();
		}
	}

	public final GameObject getObject(long id) {
		return ordered.get(id);
	}

	public final int getObjectAmount() {
		return objects.size();
	}

	public final int getActualObjectAmount() {
		return ordered.size();
	}

	public final void forEachObject(Consumer<GameObject> consumer) {
		objects.forEach(consumer);
	}

	public final void forEachActualObject(BiConsumer<Long, GameObject> consumer) {
		ordered.forEach(consumer);
	}

	private GameObject _getObject_GameObject_return;

	@SuppressWarnings("unchecked")
	public final synchronized <T> T getObject(Class<T> cls) {
		_getObject_GameObject_return = null;
		ordered.forEach((id, object) -> {
			if (object.getClass().equals(cls)) {
				_getObject_GameObject_return = object;
			}
		});
		if (_getObject_GameObject_return == null) {
			return null;
		}
		return (T) _getObject_GameObject_return;
	}

	public final synchronized GameObject getObjectN(Class<? extends GameObject> cls) {
		_getObject_GameObject_return = null;
		ordered.forEach((id, object) -> {
			if (object.getClass().equals(cls)) {
				_getObject_GameObject_return = object;
			}
		});
		return _getObject_GameObject_return;
	}

	@Override
	public void update(double delta) {
		objects.forEach(object -> {
			object.update(delta);
		});
	}

	public void render(Matrix4d projection, double delta) {
		objects.forEach(object -> {
			object.render(projection, delta);
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