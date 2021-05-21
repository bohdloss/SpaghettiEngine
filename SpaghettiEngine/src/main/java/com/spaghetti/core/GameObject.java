package com.spaghetti.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.joml.Vector3f;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

@ToClient
public abstract class GameObject implements Updatable, Renderable, Replicable {

	// Hierarchy and utility

	private static final Field c_owner;
	private static final Method c_setflag;
	private static HashMap<Integer, Integer> staticId = new HashMap<>();

	private static final synchronized int newId() {
		int index = Game.getGame().getIndex();
		Integer id = staticId.get(index);
		if (id == null) {
			id = 0;
		}
		staticId.put(index, id + 1);
		return new Random().nextInt();
	}

	static {
		Field owner = null;
		Method setflag = null;

		try {
			owner = GameComponent.class.getDeclaredField("owner");
			owner.setAccessible(true);
			setflag = GameComponent.class.getDeclaredMethod("internal_setflag", int.class, boolean.class);
			setflag.setAccessible(true);
		} catch (Throwable t) {
		}

		c_owner = owner;
		c_setflag = setflag;
	}

	// Instance methods and m_fields

	// O is attached flag
	public static final int ATTACHED = 0;
	// 1 is destroyed flag
	public static final int DESTROYED = 1;
	// 2 is delete flag
	public static final int DELETE = 2;
	// 3 is replicate flag
	public static final int REPLICATE = 3;

	private final Object flags_lock = new Object();
	private int flags;
	private int id; // This uniquely identifies any object
	private Level level;
	private GameObject parent;
	private ConcurrentHashMap<Integer, GameObject> children = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, GameComponent> components = new ConcurrentHashMap<>();

	public GameObject() {
		this.id = newId();
		internal_setflag(REPLICATE, true);
	}

	// Utility

	private final void internal_setflag(int flag, boolean value) {
		synchronized (flags_lock) {
			flags = Utils.bitAt(flags, flag, value);
		}
	}

	private final boolean internal_getflag(int flag) {
		synchronized (flags_lock) {
			return Utils.bitAt(flags, flag);
		}
	}

	public final void forEachChild(BiConsumer<Integer, GameObject> consumer) {
		children.forEach(consumer);
	}

	public final void forEachComponent(BiConsumer<Integer, GameComponent> consumer) {
		components.forEach(consumer);
	}

	// Add objects or components

	public final synchronized void addChild(GameObject object) {
		if (isDestroyed() || object == null || object.isDestroyed() || object.parent == this) {
			return;
		}

		GameObject current = this;
		while (current != null) {
			if (current == object) {
				/*
				 * In this case, the object that needs to be added is higher in the hierarchy
				 * than this one but part of the same branch this would create an infinite
				 * recursive operation which results in StackOverflowException being thrown this
				 * operation climbs up the branch trying to find 'object'
				 */
				return;
			}
			current = current.parent;
		}

		// If 'object' is attached, cut away its owners (onEndPlay opportunity here)
		if (object.internal_getflag(ATTACHED)) {
			if (object.parent == null) {
				// If this object has no parent remove it from the level directly
				if (object.level != null) {
					object.level.removeObject(object.id);
				}
			} else {
				// Otherwise remove it from its parent
				object.parent.removeChild(object.id);
			}
		}

		// Update the level pointers and add elements to level
		i_r_upd_lvl(object);

		// Finally add the object, set flags and activate trigger
		object.parent = this;
		children.put(object.id, object);
		object.internal_setflag(ATTACHED, true);
		if (isGloballyAttached()) {
			object.internal_begin();
		}
	}

	private final void i_r_upd_lvl(GameObject object) {
		object.level = level;
		if (isGloballyAttached()) {
			level.o_ordered.put(object.id, object);
			object.components.forEach((id, component) -> {
				level.c_ordered.put(component.getId(), component);
			});
		}
		object.children.forEach((id, child) -> {
			i_r_upd_lvl(child);
		});
	}

	public final synchronized void addComponent(GameComponent component) {
		if (isDestroyed() || component == null || component.isDestroyed() || component.getOwner() == this) {
			return;
		}

		if (component.getOwner() != null) {
			// onEndPlay() might happen if this component already has a parent
			component.getOwner().removeComponent(component.getId());
		}
		try {
			// Set 'this' as new owner of the component
			c_owner.set(component, this);
			// Set attached to true
			c_setflag.invoke(component, ATTACHED, true);
		} catch (Throwable t) {
		}
		components.put(component.getId(), component);

		// onBeginPlay() happens if this is globally attached
		if (isGloballyAttached()) {
			level.c_ordered.put(component.getId(), component);
			try {
				component.onBeginPlay();
			} catch (Throwable t) {
				Logger.error("Error occurred in component", t);
			}
		}
	}

	// Getter utility functions

	// Get single child by class

	@SuppressWarnings("unchecked")
	public final <T extends GameObject> T getChild(Class<T> cls) {
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return (T) obj;
			}
		}
		return null;
	}

	public final GameObject getChildN(Class<? extends GameObject> cls) {
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return obj;
			}
		}
		return null;
	}

	// Get single child by index

	public final GameObject getChildAt(int index) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (i == index) {
				return obj;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	@SuppressWarnings("unchecked")
	public final <T extends GameObject> T getChildAt(int index, Class<T> cls) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				if (i == index) {
					return (T) obj;
				}
				i++;
			}
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	public final GameObject getChild(int id) {
		return children.get(id);
	}

	// Get amount of objects by class

	public final int getChildrenAmount(Class<? extends GameObject> cls) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				i++;
			}
		}
		return i;
	}

	// Get array of objects

	@SuppressWarnings("unchecked")
	public final <T extends GameObject> T[] getChildren(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = (T) obj;
				i++;
			}
		}
		return buffer;
	}

	public final GameObject[] getChildrenN(Class<? extends GameObject> cls, GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = obj;
				i++;
			}
		}
		return buffer;
	}

	public final GameObject[] getChildren(GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			buffer[i + offset] = obj;
			i++;
		}
		return buffer;
	}

	// Get single component by class

	@SuppressWarnings("unchecked")
	public final <T extends GameComponent> T getComponent(Class<T> cls) {
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				return (T) component;
			}
		}
		return null;
	}

	public final GameComponent getComponentN(Class<? extends GameComponent> cls) {
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				return component;
			}
		}
		return null;
	}

	// Get single component by index

	public final GameComponent getComponentAt(int index) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (i == index) {
				return component;
			}
			i++;
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	@SuppressWarnings("unchecked")
	public final <T extends GameComponent> T getComponentAt(int index, Class<T> cls) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				if (i == index) {
					return (T) component;
				}
				i++;
			}
		}
		throw new IndexOutOfBoundsException("" + index);
	}

	public final GameComponent getComponent(int id) {
		return components.get(id);
	}

	// Get amounts of components by class

	public final int getComponentAmount(Class<? extends GameComponent> cls) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				i++;
			}
		}
		return i;
	}

	// Get array of components

	@SuppressWarnings("unchecked")
	public final <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				buffer[i + offset] = (T) component;
				i++;
			}
		}
		return buffer;
	}

	public final GameComponent[] getComponentsN(Class<? extends GameComponent> cls, GameComponent[] buffer,
			int offset) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				buffer[i + offset] = component;
				i++;
			}
		}
		return buffer;
	}

	public final GameComponent[] getComponents(GameComponent[] buffer, int offset) {
		int i = 0;
		for (GameComponent component : components.values()) {
			buffer[i + offset] = component;
			i++;
		}
		return buffer;
	}

	// Remove objects

	public final synchronized GameObject removeChild(int id) {
		GameObject object = children.get(id);
		if (object != null) {
			if (isGloballyAttached()) {
				// Trigger end
				object.internal_end();
				// Remove from level
				level.o_ordered.remove(id);
			}

			// Remove from list, set flags
			object.parent = null;
			children.remove(id);
			object.internal_setflag(ATTACHED, false);

			return object;
		}
		return null;
	}

	public final synchronized GameObject removeChild(GameObject object) {
		return removeChild(object.id);
	}

	public final synchronized void removeChildren() {
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			removeChild(object.id);
		}
	}

	// Remove components

	public final synchronized GameComponent removeComponent(int id) {
		GameComponent component = components.get(id);
		if (component != null) {
			if (isGloballyAttached()) {
				// Trigger end
				try {
					component.onEndPlay();
				} catch (Throwable t) {
					Logger.error("Error occurred in component", t);
				}
				// Remove from level
				level.c_ordered.remove(id);
			}

			// Remove from list, set flags
			components.remove(id);
			try {
				// Set 'null' as new owner of the component
				c_owner.set(component, null);
				// Set attached to false
				c_setflag.invoke(component, ATTACHED, false);
			} catch (Throwable t) {
			}

			return component;
		}
		return null;
	}

	public final synchronized GameComponent removeComponent(GameComponent component) {
		return removeComponent(component.getId());
	}

	public final synchronized void removeComponents() {
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			removeComponent(component.getId());
		}
	}

	// Delete objects

	public final synchronized boolean deleteChild(int id) {
		GameObject get = children.get(id);
		if (get != null) {
			get.destroy();
			return true;
		}
		return false;
	}

	public final synchronized boolean deleteChild(GameObject child) {
		return deleteChild(child.id);
	}

	public final synchronized void deleteChildren() {
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			deleteChild(object.getId());
		}
	}

	// Delete components

	public final synchronized GameComponent deleteComponent(int id) {
		GameComponent component = components.get(id);
		if (component != null) {
			component.destroy();
			return component;
		}
		return null;
	}

	public final synchronized GameComponent deleteComponent(GameComponent component) {
		return deleteComponent(component.getId());
	}

	public final synchronized void deleteComponents() {
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			deleteComponent(component.getId());
		}
	}

	// Self destroy methods

	public synchronized final void destroy() {
		if (isDestroyed()) {
			return;
		}
		internal_destroy();
	}

	private final void destroySimple() {
		try {
			onDestroy();
		} catch (Throwable t) {
			Logger.error("Error occurred in object", t);
		}
		if (parent == null) {
			if (level != null) {
				level.removeObject(id);
			}
		} else {
			parent.removeChild(id);
		}
		level = null;
		parent = null;
		internal_setflag(DESTROYED, true);
	}

	protected final void internal_begin() {
		try {
			onBeginPlay();
		} catch (Throwable t) {
			Logger.error("Error occurred in object", t);
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			try {
				component.onBeginPlay();
			} catch (Throwable t) {
				Logger.error("Error occurred in component", t);
			}
		}
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object.internal_begin();
		}
	}

	protected final void internal_end() {
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object.internal_end();
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			try {
				component.onEndPlay();
			} catch (Throwable t) {
				Logger.error("Error occurred in component", t);
			}
		}
		try {
			onEndPlay();
		} catch (Throwable t) {
			Logger.error("Error occurred in component", t);
		}
	}

	protected final void internal_destroy() {
		for (Object obj : children.values().toArray()) {
			GameObject child = (GameObject) obj;
			child.internal_destroy();
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.destroy();
		}
		destroySimple();
	}

	// Getters

	public final int getChildrenAmount() {
		return children.size();
	}

	public final int getComponentAmount() {
		return components.size();
	}

	public final GameObject getParent() {
		return parent;
	}

	public final Level getLevel() {
		return level;
	}

	public final Game getGame() {
		return level == null ? Game.getGame() : level.getGame();
	}

	public final int getId() {
		return id;
	}

	public final GameObject getBase() {
		GameObject last = this;
		while (true) {
			if (last.parent == null) {
				return last;
			} else {
				last = last.parent;
			}
		}
	}

	public final boolean isDestroyed() {
		return internal_getflag(DESTROYED);
	}

	public final boolean isLocallyAttached() {
		return internal_getflag(ATTACHED);
	}

	public final boolean isGloballyAttached() {
		if (!internal_getflag(ATTACHED)) {
			return false;
		}
		GameObject obj = parent;
		while (obj != null) {
			if (!obj.internal_getflag(ATTACHED)) {
				return false;
			}
			obj = obj.parent;
		}
		return true;
	}

	// Override for more precise control
	public boolean getReplicateFlag() {
		return internal_getflag(REPLICATE);
	}

	// World interaction

	protected final Vector3f relativePosition = new Vector3f();
	protected final Vector3f relativeScale = new Vector3f(1, 1, 1);
	protected final Vector3f relativeRotation = new Vector3f();

	// Position getters

	public final void getRelativePosition(Vector3f pointer) {
		pointer.set(relativePosition);
	}

	public final void getWorldPosition(Vector3f pointer) {
		pointer.zero();

		GameObject last = this;
		while (last != null) {
			pointer.add(last.relativePosition);
			last = last.parent;
		}
	}

	public final float getRelativeX() {
		return relativePosition.x;
	}

	public final float getRelativeY() {
		return relativePosition.y;
	}

	public final float getRelativeZ() {
		return relativePosition.z;
	}

	public final float getWorldX() {
		float x = 0;

		GameObject last = this;
		while (last != null) {
			x += last.relativePosition.x;
			last = last.parent;
		}
		return x;
	}

	public final float getWorldY() {
		float y = 0;

		GameObject last = this;
		while (last != null) {
			y += last.relativePosition.y;
			last = last.parent;
		}
		return y;
	}

	public final float getWorldZ() {
		float z = 0;

		GameObject last = this;
		while (last != null) {
			z += last.relativePosition.z;
			last = last.parent;
		}
		return z;
	}

	// Position setters

	public final void setRelativePosition(Vector3f vec) {
		setRelativePosition(vec.x, vec.y, vec.z);
	}

	public final void setRelativePosition(float x, float y, float z) {
		relativePosition.set(x, y, z);
	}

	public final void setWorldPosition(Vector3f vec) {
		setWorldPosition(vec.x, vec.y, vec.z);
	}

	public final void setWorldPosition(float x, float y, float z) {
		Vector3f vec3 = new Vector3f();
		getWorldPosition(vec3);

		float xdiff = vec3.x - x;
		float ydiff = vec3.y - y;
		float zdiff = vec3.z - z;

		setRelativePosition(relativePosition.x - xdiff, relativePosition.y - ydiff, relativePosition.z - zdiff);
	}

	public final void setRelativeX(float x) {
		setRelativePosition(x, relativePosition.y, relativePosition.z);
	}

	public final void setRelativeY(float y) {
		setRelativePosition(relativePosition.x, y, relativePosition.z);
	}

	public final void setRelativeZ(float z) {
		setRelativePosition(relativePosition.x, relativePosition.y, z);
	}

	public final void setWorldX(float worldx) {
		Vector3f vec3 = new Vector3f();
		getWorldPosition(vec3);
		setWorldPosition(worldx, vec3.y, vec3.z);
	}

	public final void setWorldY(float worldy) {
		Vector3f vec3 = new Vector3f();
		getWorldPosition(vec3);
		setWorldPosition(vec3.x, worldy, vec3.z);
	}

	public final void setWorldZ(float worldz) {
		Vector3f vec3 = new Vector3f();
		getWorldPosition(vec3);
		setWorldPosition(vec3.x, vec3.y, worldz);
	}

	// Scale getters

	public final void getRelativeScale(Vector3f pointer) {
		pointer.set(relativeScale);
	}

	public final void getWorldScale(Vector3f pointer) {
		pointer.set(1);

		GameObject last = this;
		while (last != null) {
			pointer.mul(last.relativeScale);
			last = last.parent;
		}
	}

	public final float getXScale() {
		return relativeScale.x;
	}

	public final float getYScale() {
		return relativeScale.y;
	}

	public final float getZScale() {
		return relativeScale.z;
	}

	public final float getWorldXScale() {
		float x = 0;

		GameObject last = this;
		while (last != null) {
			x *= last.relativeScale.x;
			last = last.parent;
		}
		return x;
	}

	public final float getWorldYScale() {
		float y = 0;

		GameObject last = this;
		while (last != null) {
			y *= last.relativeScale.y;
			last = last.parent;
		}
		return y;
	}

	public final float getWorldZScale() {
		float z = 0;

		GameObject last = this;
		while (last != null) {
			z *= last.relativeScale.z;
			last = last.parent;
		}
		return z;
	}

	// Scale setters

	public final void setRelativeScale(Vector3f vec) {
		setRelativeScale(vec.x, vec.y, vec.z);
	}

	public final void setRelativeScale(float x, float y, float z) {
		relativeScale.set(x, y, z);
	}

	public final void setWorldScale(Vector3f vec) {
		setWorldScale(vec.x, vec.y, vec.z);
	}

	public final void setWorldScale(float x, float y, float z) {
		Vector3f vec3 = new Vector3f();
		getWorldScale(vec3);

		float xdiff = vec3.x / x;
		float ydiff = vec3.y / y;
		float zdiff = vec3.z / z;

		setRelativeScale(relativeScale.x / xdiff, relativeScale.y / ydiff, relativeScale.z / zdiff);
	}

	public final void setXScale(float x) {
		setRelativeScale(x, relativeScale.y, relativeScale.z);
	}

	public final void setYScale(float y) {
		setRelativeScale(relativeScale.x, y, relativeScale.z);
	}

	public final void setZScale(float z) {
		setRelativeScale(relativeScale.x, relativeScale.y, z);
	}

	public final void setWorldXScale(float worldx) {
		Vector3f vec3 = new Vector3f();
		getWorldScale(vec3);
		setWorldScale(worldx, vec3.y, vec3.z);
	}

	public final void setWorldYScale(float worldy) {
		Vector3f vec3 = new Vector3f();
		getWorldScale(vec3);
		setWorldScale(vec3.x, worldy, vec3.z);
	}

	public final void setWorldZScale(float worldz) {
		Vector3f vec3 = new Vector3f();
		getWorldScale(vec3);
		setWorldScale(vec3.x, vec3.y, worldz);
	}

	// Rotation getters

	public final void getRelativeRotation(Vector3f pointer) {
		pointer.set(relativeRotation);
	}

	public final void getWorldRotation(Vector3f pointer) {
		pointer.zero();

		GameObject last = this;
		while (last != null) {
			pointer.add(last.relativeRotation);
			last = last.parent;
		}
	}

	public final float getYaw() {
		return relativeRotation.x;
	}

	public final float getPitch() {
		return relativeRotation.y;
	}

	public final float getRoll() {
		return relativeRotation.z;
	}

	public final float getWorldYaw() {
		float yaw = 0;

		GameObject last = this;
		while (last != null) {
			yaw += last.relativeRotation.x;
			last = last.parent;
		}
		return yaw;
	}

	public final float getWorldPitch() {
		float pitch = 0;

		GameObject last = this;
		while (last != null) {
			pitch += last.relativeRotation.y;
			last = last.parent;
		}
		return pitch;
	}

	public final float getWorldRoll() {
		float roll = 0;

		GameObject last = this;
		while (last != null) {
			roll += last.relativeRotation.z;
			last = last.parent;
		}
		return roll;
	}

	// Rotation setters

	public final void setRelativeRotation(Vector3f vec) {
		setRelativeRotation(vec.x, vec.y, vec.z);
	}

	public final void setRelativeRotation(float x, float y, float z) {
		relativeRotation.set(x, y, z);
	}

	public final void setWorldRotation(Vector3f vec) {
		setWorldRotation(vec.x, vec.y, vec.z);
	}

	public final void setWorldRotation(float x, float y, float z) {
		Vector3f vec3 = new Vector3f();
		getWorldRotation(vec3);

		float xdiff = vec3.x - x;
		float ydiff = vec3.y - y;
		float zdiff = vec3.z - z;

		setRelativeRotation(relativeRotation.x - xdiff, relativeRotation.y - ydiff, relativeRotation.z - zdiff);
	}

	public final void setYaw(float yaw) {
		setRelativeRotation(yaw, relativeRotation.y, relativeRotation.z);
	}

	public final void setPitch(float pitch) {
		setRelativeRotation(relativeRotation.x, pitch, relativeRotation.z);
	}

	public final void setRoll(float roll) {
		setRelativeRotation(relativeRotation.x, relativeRotation.y, roll);
	}

	public final void setWorldYaw(float worldyaw) {
		Vector3f vec3 = new Vector3f();
		getWorldRotation(vec3);
		setWorldRotation(worldyaw, vec3.y, vec3.z);
	}

	public final void setWorldPitch(float worldpitch) {
		Vector3f vec3 = new Vector3f();
		getWorldRotation(vec3);
		setWorldRotation(vec3.x, worldpitch, vec3.z);
	}

	public final void setWorldRoll(float worldroll) {
		Vector3f vec3 = new Vector3f();
		getWorldRotation(vec3);
		setWorldRotation(vec3.x, vec3.y, worldroll);
	}

	// Interface methods

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	@Override
	public final void update(float delta) {
		components.forEach((id, component) -> {
			if (component != null) {
				component.update(delta);
			}
		});

		commonUpdate(delta);
		if (getGame().isClient()) {
			clientUpdate(delta);
		} else {
			serverUpdate(delta);
		}
		children.forEach((id, object) -> {
			if (object != null) {
				object.update(delta);
			}
		});
	}

	protected void serverUpdate(float delta) {
		// WARNING: NONE of the code in this method
		// should EVER try to interact with render code
		// or other objects that require an opngGL context
		// as it will trigger errors or, in the worst
		// scenario, a SIGSEGV signal (Segmentation fault)
		// shutting down the entire server
		// (Which might even be a dedicated server as a whole)
	}

	protected void clientUpdate(float delta) {
		// Here doing such things may still cause
		// exceptions or weird and hard to debug errors
		// so by design it is best not to include such
		// code in update methods
	}

	protected void commonUpdate(float delta) {
		// Happens on both server and client regardless
		// So follow all the warnings reported on the serverUpdate
		// method plus the ones on clientUpdate
	}

	@Override
	public void render(Camera renderer, float delta) {
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		if (relativePosition.x == 0 && relativePosition.y == 0 && relativePosition.z == 0) {
			buffer.putBoolean(true);
		} else {
			buffer.putBoolean(false);
			buffer.putFloat(relativePosition.x);
			buffer.putFloat(relativePosition.y);
			buffer.putFloat(relativePosition.z);
		}

		if (relativeScale.x == 0 && relativeScale.y == 0 && relativeScale.z == 0) {
			buffer.putBoolean(true);
		} else {
			buffer.putBoolean(false);
			buffer.putFloat(relativeScale.x);
			buffer.putFloat(relativeScale.y);
			buffer.putFloat(relativeScale.z);
		}

		if (relativeRotation.x == 0 && relativeRotation.y == 0 && relativeRotation.z == 0) {
			buffer.putBoolean(true);
		} else {
			buffer.putBoolean(false);
			buffer.putFloat(relativeRotation.x);
			buffer.putFloat(relativeRotation.y);
			buffer.putFloat(relativeRotation.z);
		}
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		if (buffer.getBoolean()) {
			relativePosition.x = 0;
			relativePosition.y = 0;
			relativePosition.z = 0;
		} else {
			relativePosition.x = buffer.getFloat();
			relativePosition.y = buffer.getFloat();
			relativePosition.z = buffer.getFloat();
		}

		if (buffer.getBoolean()) {
			relativeScale.x = 0;
			relativeScale.y = 0;
			relativeScale.z = 0;
		} else {
			relativeScale.x = buffer.getFloat();
			relativeScale.y = buffer.getFloat();
			relativeScale.z = buffer.getFloat();
		}

		if (buffer.getBoolean()) {
			relativeRotation.x = 0;
			relativeRotation.y = 0;
			relativeRotation.z = 0;
		} else {
			relativeRotation.x = buffer.getFloat();
			relativeRotation.y = buffer.getFloat();
			relativeRotation.z = buffer.getFloat();
		}
	}

	protected final void setReplicateFlag(boolean flag) {
		internal_setflag(REPLICATE, flag);
	}

	// Event dispatching

	protected final void raiseSignal(long signal) {
		getGame().getEventDispatcher().raiseSignal(this, signal);
	}

	protected final void raiseEvent(GameEvent event) {
		getGame().getEventDispatcher().raiseEvent(this, event);
	}

	protected final void raiseIntention(long intention) {
		getGame().getEventDispatcher().raiseIntention(this, intention);
	}

}
