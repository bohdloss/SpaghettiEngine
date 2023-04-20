package com.spaghetti.world;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.spaghetti.core.Game;
import com.spaghetti.input.Updatable;
import com.spaghetti.utils.ReflectionUtil;

public final class Level implements Updatable {

	private static final Field o_level = ReflectionUtil.getPrivateField(GameObject.class, "level");
	private static final Field o_parent = ReflectionUtil.getPrivateField(GameObject.class, "parent");
	private static final Method o_setflag = ReflectionUtil.getPrivateMethod(GameObject.class, "internal_setflag", int.class,
			boolean.class);

	protected boolean destroyed;
	protected boolean attached;
	protected final Game game;
	protected final ArrayList<GameObject> objects = new ArrayList<>();
	protected final ConcurrentHashMap<Integer, GameObject> o_ordered = new ConcurrentHashMap<>();
	protected final ConcurrentHashMap<Integer, GameComponent> c_ordered = new ConcurrentHashMap<>();

	protected final String name;

	public Level(Game game, String name) {
		if(name == null) {
			throw new NullPointerException();
		}
		this.game = game;
		this.name = name;
	}

	protected void onBeginPlay() {
		for (GameObject obj : objects) {
			obj.onbegin_forward();
		}
	}

	protected void onEndPlay() {
		for (GameObject obj : objects) {
			obj.onend_forward();
		}
	}

	protected void onDestroy() {
	}

	public final void destroy() {
		if (isDestroyed()) {
			return;
		}
		onDestroy();
		synchronized(objects) {
			for (Object obj : objects.toArray()) {
				GameObject go = (GameObject) obj;
				go.destroy();
			}
		}
		destroyed = true;
	}

	public final synchronized void addObject(GameObject object) {
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
		update_level(object);

		// Finally add to list, set flags, activate triggers
		synchronized(objects) {
			objects.add(object);
		}
		try {
			o_setflag.invoke(object, GameObject.ATTACHED, true);
			o_parent.set(object, null);
		} catch (Throwable t) {
		}
		if (isAttached()) {
			object.onbegin_forward();
		}
	}

	private final void update_level(GameObject object) {
		try {
			o_level.set(object, this);
		} catch (Throwable t) {
		}
		o_ordered.put(object.getId(), object);
		object.forEachComponent((id, component) -> {
			c_ordered.put(component.getId(), component);
		});
		object.forEachChild((id, child) -> {
			update_level(child);
		});
	}

	public final synchronized GameObject removeObject(int id) {
		GameObject object = o_ordered.get(id);
		if (!objects.contains(object) || object == null) {
			return null;
		}

		object.onend_forward();
		synchronized(objects) {
			objects.remove(object);
		}
		o_ordered.remove(id);
		try {
			o_parent.set(object, null);
			o_level.set(object, null);
			o_setflag.invoke(object, GameObject.ATTACHED, false);
		} catch (Throwable t) {
		}
		return object;
	}

	public final synchronized boolean deleteObject(int id) {
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

	public final GameObject getObject(int id) {
		return o_ordered.get(id);
	}

	public final GameComponent getComponent(int id) {
		return c_ordered.get(id);
	}

	public final int getObjectAmount() {
		return objects.size();
	}

	public final int getActualObjectAmount() {
		return o_ordered.size();
	}

	public final int getComponentAmount() {
		return c_ordered.size();
	}

	public final void forEachObject(Consumer<GameObject> consumer) {
		synchronized(objects) {
			objects.forEach(consumer);
		}
	}

	public final void forEachActualObject(BiConsumer<Integer, GameObject> consumer) {
		o_ordered.forEach(consumer);
	}

	public final void forEachComponent(BiConsumer<Integer, GameComponent> consumer) {
		c_ordered.forEach(consumer);
	}

	@Override
	public final void update(float delta) {
		try {
			// Update level
			objects.forEach(object -> {
				if (object != null) {
					object.update(delta);
				}
			});
		} catch (ConcurrentModificationException e) {
		}
	}

	public final boolean isDestroyed() {
		return destroyed;
	}

	public final boolean isAttached() {
		return attached;
	}

	// Getter utility functions

	// Get single object by class

	@SuppressWarnings("unchecked")
	public final <T extends GameObject> T getObject(Class<T> cls) {
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return (T) obj;
			}
		}
		return null;
	}

	public final GameObject getObjectN(Class<? extends GameObject> cls) {
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return obj;
			}
		}
		return null;
	}

	// Get single object by index

	public final GameObject getObjectAt(int index) {
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
	public final <T extends GameObject> T getObjectAt(int index, Class<T> cls) {
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

	public final int getObjectAmount(Class<? extends GameObject> cls) {
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
	public final <T extends GameObject> T[] getObjects(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = (T) obj;
				i++;
			}
		}
		return buffer;
	}

	public final GameObject[] getObjectsN(Class<? extends GameObject> cls, GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = obj;
				i++;
			}
		}
		return buffer;
	}

	public final GameObject[] getObjects(GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : o_ordered.values()) {
			buffer[i + offset] = obj;
			i++;
		}
		return buffer;
	}

	// Get single component by class

	@SuppressWarnings("unchecked")
	public final <T extends GameComponent> T getComponent(Class<T> cls) {
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				return (T) comp;
			}
		}
		return null;
	}

	public final GameComponent getComponentN(Class<? extends GameComponent> cls) {
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				return comp;
			}
		}
		return null;
	}

	// Get single component by index

	public final GameComponent getComponentAt(int index) {
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
	public final <T extends GameComponent> T getComponentAt(int index, Class<T> cls) {
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

	public final int getComponentAmount(Class<? extends GameComponent> cls) {
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
	public final <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				buffer[i + offset] = (T) comp;
				i++;
			}
		}
		return buffer;
	}

	public final GameComponent[] getComponentsN(Class<? extends GameComponent> cls, GameComponent[] buffer,
			int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			if (cls.isAssignableFrom(comp.getClass())) {
				buffer[i + offset] = comp;
				i++;
			}
		}
		return buffer;
	}

	public final GameComponent[] getComponents(GameComponent[] buffer, int offset) {
		int i = 0;
		for (GameComponent comp : c_ordered.values()) {
			buffer[i + offset] = comp;
			i++;
		}
		return buffer;
	}

	// Other getters / setters

	public Game getGame() {
		return game;
	}

	public String getName() {
		return name;
	}

}