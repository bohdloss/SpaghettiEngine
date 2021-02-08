package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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

	protected synchronized final void add_object(GameObject object) {
		objects.add(object);
		ordered.put(object.getId(), object);
	}

	public synchronized final void addObject(GameObject object) {
		if (objects.contains(object)) {
			return;
		}

		GameObject.rebuildObject(null, object);

		if (object.getParent() == null) {
			objects.add(object);
		}
		ordered.put(object.getId(), object);
	}

	public synchronized final GameObject removeObject(long id) {
		GameObject get = ordered.get(id);
		if (get != null) {
			if (get.getParent() != null) {
				get.getParent().removeChild(get);
			} else {
				get._end();
				objects.remove(get);
				ordered.remove(id);
			}
		}
		return get;
	}

	public synchronized final boolean deleteObject(long id) {
		GameObject get = ordered.get(id);
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

	public final GameObject getObject(long id) {
		return ordered.get(id);
	}

	public final int getObjectAmount() {
		return objects.size();
	}

	public final int getActualObjectAmount() {
		return ordered.size();
	}

	public final int getComponentAmount() {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			i += obj.getComponentAmount();
		}
		return i;
	}
	
	public final void forEachObject(Consumer<GameObject> consumer) {
		objects.forEach(consumer);
	}

	public final void forEachActualObject(BiConsumer<Long, GameObject> consumer) {
		ordered.forEach(consumer);
	}
	
	@Override
	public void update(double delta) {
		objects.forEach(object -> {
			object.update(delta);
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

	// Getter utility functions
	
	// Get single object by class
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T getObject(Class<T> cls) {
		for(GameObject obj : ordered.values()) {
			if (obj.getClass().equals(cls)) {
				return (T) obj;
			}
		}
		return null;
	}

	public final synchronized GameObject getObjectN(Class<? extends GameObject> cls) {
		for(GameObject obj : ordered.values()) {
			if (obj.getClass().equals(cls)) {
				return obj;
			}
		}
		return null;
	}

	// Get single object by index
	
	public final synchronized GameObject getObject(int index) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			if(i == index) {
				return obj;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T getObjectIndex(int index, Class<T> cls) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			if(obj.getClass().equals(cls)) {
				if(i == index) {
					return (T) obj;
				}
				i++;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}
	
	// Get amount of objects by class
	
	public final synchronized int getObjectAmount(Class<? extends GameObject> cls) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			if(obj.getClass().equals(cls)) {
				i++;
			}
		}
		return i;
	}
	
	// Get array of objects
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T[] getObjects(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			if(obj.getClass().equals(cls)) {
				buffer[i + offset] = (T) obj;
			}
			i++;
		}
		return buffer;
	}
	
	public final synchronized GameObject[] getObjectsN(Class<? extends GameObject> cls, GameObject[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			if(obj.getClass().equals(cls)) {
				buffer[i + offset] = obj;
			}
			i++;
		}
		return buffer;
	}
	
	public final synchronized GameObject[] getObjects(GameObject[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			buffer[i + offset] = obj;
			i++;
		}
		return buffer;
	}
	
	// Get single component by class
	
	public final synchronized <T extends GameComponent> T getComponent(Class<T> cls) {
		for(GameObject obj : ordered.values()) {
			T found = obj.getComponent(cls);
			if(found != null) {
				return found;
			}
		}
		return null;
	}
	
	public final synchronized GameComponent getComponentN(Class<? extends GameComponent> cls) {
		for(GameObject obj : ordered.values()) {
			GameComponent found = obj.getComponentN(cls);
			if(found != null) {
				return found;
			}
		}
		return null;
	}
	
	// Get single component by index
	
	public final synchronized GameComponent getComponent(int index) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			int compAmount = obj.getComponentAmount();
			if(index >= i && i + compAmount > index) {
				GameComponent found = obj.getComponent(index - i);
				return found;
			}
			i += compAmount;
		}
		throw new IndexOutOfBoundsException("" + index);
	}
	
	public final synchronized <T extends GameComponent> T getComponent(int index, Class<T> cls) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			int compAmount = obj.getComponentAmount(cls);
			if(index >= i && i + compAmount > index) {
				T found = obj.getComponent(index - i, cls);
				return found;
			}
			i += compAmount;
		}
		throw new IndexOutOfBoundsException("" + index);
	}
	
	// Get amounts of components by class
	
	public final synchronized int getComponentAmount(Class<? extends GameComponent> cls) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			i += obj.getComponentAmount(cls);
		}
		return i;
	}
	
	// Get array of objects
	
	public final synchronized <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			obj.getComponents(cls, buffer, offset + i);
			i += obj.getComponentAmount(cls);
		}
		return buffer;
	}
	
	public final synchronized GameComponent[] getComponentsN(Class<? extends GameComponent> cls,
			GameComponent[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			obj.getComponentsN(cls, buffer, offset + i);
			i += obj.getComponentAmount(cls);
		}
		return buffer;
	}
	
	public final synchronized GameComponent[] getComponents(GameComponent[] buffer, int offset) {
		int i = 0;
		for(GameObject obj : ordered.values()) {
			obj.getComponents(buffer, offset + i);
			i += obj.getComponentAmount();
		}
		return buffer;
	}
	
}