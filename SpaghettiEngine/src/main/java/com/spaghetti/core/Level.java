package com.spaghetti.core;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.Updatable;
import com.spaghetti.objects.Camera;

public final class Level implements Updatable {

	private static final Field o_level;
	private static final Field o_parent;
	private static final Method o_setflag;

	static {
		Field level = null;
		Field parent = null;
		Method setflag = null;

		try {
			level = GameObject.class.getDeclaredField("level");
			level.setAccessible(true);
			setflag = GameObject.class.getDeclaredMethod("internal_setflag", int.class, boolean.class);
			setflag.setAccessible(true);
			parent = GameObject.class.getDeclaredField("parent");
			parent.setAccessible(true);
		} catch (Throwable t) {
		}

		o_level = level;
		o_parent = parent;
		o_setflag = setflag;
	}

	protected boolean destroyed;
	protected Game source;
	protected ArrayList<GameObject> objects = new ArrayList<>();
	protected ConcurrentHashMap<Long, GameObject> o_ordered = new ConcurrentHashMap<>();
	protected ConcurrentHashMap<Long, GameComponent> c_ordered = new ConcurrentHashMap<>();
	protected Camera activeCamera;
	protected Controller activeInput;

	public Level() {
	}

	public Game getGame() {
		return source;
	}

	public void destroy() {
		if (isDestroyed()) {
			return;
		}
		for (Object obj : objects.toArray()) {
			GameObject go = (GameObject) obj;
			go.destroy();
		}
		destroyed = true;
	}

	public synchronized void addObject(GameObject object) {
		if (objects.contains(object) || object == null || object.isDestroyed() || isDestroyed()) {
			return;
		}

		// If 'object' is attached, cut away its owners (onEndPlay opportunity here)
		if (object.isLocallyAttached()) {
			if (object.getParent() == null) {
				// If this object has no parent remove it from the level directly
				if (object.getLevel() != null) {
					object.getLevel().removeObject(object.getId());
				}
			} else {
				// Otherwise remove it from its parent
				object.getParent().removeChild(object.getId());
			}
		}

		// Update level pointers and level lists
		i_r_upd_lvl(object);

		// Finally add to list, set flags, activate triggers
		objects.add(object);
		try {
			o_setflag.invoke(object, GameObject.ATTACHED, true);
			o_parent.set(object, null);
		} catch (Throwable t) {
		}
		object.internal_begin();
	}

	private final void i_r_upd_lvl(GameObject object) {
		try {
			o_level.set(object, this);
		} catch (Throwable t) {
		}
		o_ordered.put(object.getId(), object);
		object.forEachComponent((id, component) -> {
			c_ordered.put(component.getId(), component);
		});
		object.forEachChild((id, child) -> {
			i_r_upd_lvl(child);
		});
	}

	public synchronized GameObject removeObject(long id) {
		GameObject object = o_ordered.get(id);
		if (!objects.contains(object)) {
			return null;
		}
		if (object != null) {
			object.internal_end();
			objects.remove(object);
			o_ordered.remove(id);
			try {
				o_parent.set(object, null);
				o_setflag.invoke(object, GameObject.ATTACHED, false);
			} catch (Throwable t) {
			}
		}
		return object;
	}

	public synchronized boolean deleteObject(long id) {
		GameObject get = o_ordered.get(id);
		if (!objects.contains(get)) {
			return false;
		}
		if (get != null) {
			get.destroy();
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

	public boolean isDestroyed() {
		return destroyed;
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