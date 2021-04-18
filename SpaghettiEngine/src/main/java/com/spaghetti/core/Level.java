package com.spaghetti.core;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.Updatable;
import com.spaghetti.objects.Camera;

public final class Level implements Updatable {

	protected Game source;
	protected ArrayList<GameObject> objects = new ArrayList<>();
	protected HashMap<Long, GameObject> o_ordered = new HashMap<>();
	protected HashMap<Long, GameComponent> c_ordered = new HashMap<>();
	protected Camera activeCamera;
	protected Controller activeInput;

	public Level() {
	}

	public Game getGame() {
		return source;
	}

	public void destroy() {
		for (Object obj : objects.toArray()) {
			GameObject go = (GameObject) obj;
			go.destroy();
		}
	}

	protected synchronized void add_object(GameObject object) {
		objects.add(object);
		o_ordered.put(object.getId(), object);
	}

	public synchronized void addObject(GameObject object) {
		if (objects.contains(object) || object == null || object.isDestroyed()) {
			return;
		}

		GameObject.internal_attachobj(this, null, object);
	}

	public synchronized GameObject removeObject(long id) {
		GameObject obj = o_ordered.get(id);
		if (obj != null) {
			if (obj.getParent() != null) {
				obj.getParent().removeChild(obj);
			} else {
				obj.internal_end();
				objects.remove(obj);
				o_ordered.remove(id);
				GameObject.internal_detach(obj);
			}
		}
		return obj;
	}

	public synchronized boolean deleteObject(long id) {
		GameObject get = o_ordered.get(id);
		if (get != null) {
			if (get.getParent() != null) {
				get.getParent().deleteChild(get);
			} else {
				get.destroy();
			}

			return true;
		}
		return false;
	}

	public GameObject getObject(long id) {
		return o_ordered.get(id);
	}

	public GameComponent getComponent(long id) {
		return c_ordered.get(id);
	}

	public int getObjectAmount() {
		return objects.size();
	}

	public int getActualObjectAmount() {
		return o_ordered.size();
	}

	public int getComponentAmount() {
		return c_ordered.size();
	}

	public void forEachObject(Consumer<GameObject> consumer) {
		objects.forEach(consumer);
	}

	public void forEachActualObject(BiConsumer<Long, GameObject> consumer) {
		o_ordered.forEach(consumer);
	}

	public void forEachComponent(BiConsumer<Long, GameComponent> consumer) {
		c_ordered.forEach(consumer);
	}

	@Override
	public void update(double delta) {
		try {
			objects.forEach(object -> {
				if (object != null) {
					object.update(delta);
				}
			});
		} catch (ConcurrentModificationException e) {
		}
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
		if (camera == null || source.isHeadless() || activeCamera == camera) {
			return;
		}
		if (activeCamera != null) {
			detachCamera();
		}
		camera.calcScale();
		activeCamera = camera;
	}

	public Controller getController() {
		return activeInput;
	}

	public void detachController() {
		if (activeInput == null) {
			return;
		}
		source.getWindow().getInputDispatcher().unregisterListener(activeInput);
		activeInput = null;
	}

	public void attachController(Controller controller) {
		if (controller == null || source.isHeadless() || activeInput == controller) {
			return;
		}
		if (activeInput != null) {
			detachController();
		}
		source.getWindow().getInputDispatcher().registerListener(controller);
		this.activeInput = controller;
	}

	// Getter utility functions

	// Get single object by class

	@SuppressWarnings("unchecked")
	public <T extends GameObject> T getObject(Class<T> cls) {
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return (T) obj;
			}
		}
		return null;
	}

	public GameObject getObjectN(Class<? extends GameObject> cls) {
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return obj;
			}
		}
		return null;
	}

	// Get single object by index

	public GameObject getObject(int index) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (i == index) {
				return obj;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	@SuppressWarnings("unchecked")
	public <T extends GameObject> T getObject(int index, Class<T> cls) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				if (i == index) {
					return (T) obj;
				}
				i++;
			}
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	// Get amount of objects by class

	public int getObjectAmount(Class<? extends GameObject> cls) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				i++;
			}
		}
		return i;
	}

	// Get array of objects

	@SuppressWarnings("unchecked")
	public <T extends GameObject> T[] getObjects(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = (T) obj;
				i++;
			}
		}
		return buffer;
	}

	public GameObject[] getObjectsN(Class<? extends GameObject> cls, GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = obj;
				i++;
			}
		}
		return buffer;
	}

	public GameObject[] getObjects(GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			buffer[i + offset] = obj;
			i++;
		}
		return buffer;
	}

	// Get single component by class

	@SuppressWarnings("unchecked")
	public <T extends GameComponent> T getComponent(Class<T> cls) {
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				return (T) comp;
			}
		}
		return null;
	}

	public GameComponent getComponentN(Class<? extends GameComponent> cls) {
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				return comp;
			}
		}
		return null;
	}

	// Get single component by index

	public GameComponent getComponent(int index) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (i == index) {
				return comp;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	@SuppressWarnings("unchecked")
	public <T extends GameComponent> T getComponent(int index, Class<T> cls) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				if (i == index) {
					return (T) comp;
				}
				i++;
			}
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	// Get amounts of components by class

	public int getComponentAmount(Class<? extends GameComponent> cls) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				i++;
			}
		}
		return i;
	}

	// Get array of components

	@SuppressWarnings("unchecked")
	public <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				buffer[i + offset] = (T) comp;
				i++;
			}
		}
		return buffer;
	}

	public GameComponent[] getComponentsN(Class<? extends GameComponent> cls, GameComponent[] buffer, int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				buffer[i + offset] = comp;
				i++;
			}
		}
		return buffer;
	}

	public GameComponent[] getComponents(GameComponent[] buffer, int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			buffer[i + offset] = comp;
			i++;
		}
		return buffer;
	}

}