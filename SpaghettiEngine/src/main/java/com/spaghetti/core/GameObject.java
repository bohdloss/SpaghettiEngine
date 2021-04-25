package com.spaghetti.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class GameObject implements Updatable, Renderable, Replicable {

	// Hierarchy and utility

	private static final Field c_owner;
	private static final Method c_setflag;
	private static HashMap<Integer, Long> staticId = new HashMap<>();

	private static final synchronized long newId() {
		int index = Game.getGame().getIndex();
		Long id = staticId.get(index);
		if (id == null) {
			id = 0l;
		}
		staticId.put(index, id + 1l);
		return new Random().nextLong();
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
	private long id; // This uniquely identifies any object
	private Level level;
	private GameObject parent;
	private ConcurrentHashMap<Long, GameObject> children = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Long, GameComponent> components = new ConcurrentHashMap<>();

	public GameObject() {
		this.id = newId();
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

	public final void forEachChild(BiConsumer<Long, GameObject> consumer) {
		children.forEach(consumer);
	}

	public final void forEachComponent(BiConsumer<Long, GameComponent> consumer) {
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

	public final GameObject getChild(int index) {
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
	public final <T extends GameObject> T getChild(int index, Class<T> cls) {
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

	public final GameObject getChild(long id) {
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

	public final GameComponent getComponent(int index) {
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
	public final <T extends GameComponent> T getComponent(int index, Class<T> cls) {
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

	public final GameComponent getComponent(long id) {
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

	public final synchronized GameObject removeChild(long id) {
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

	public final synchronized GameComponent removeComponent(long id) {
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

	public final synchronized boolean deleteChild(long id) {
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

	public final synchronized GameComponent deleteComponent(long id) {
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
		return level.getGame();
	}

	public final long getId() {
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

	// World interaction

	protected final Vector3d relativePosition = new Vector3d();
	protected final Vector3d relativeScale = new Vector3d(1, 1, 1);
	protected final Vector3d relativeRotation = new Vector3d();

	// Position getters

	public final void getRelativePosition(Vector3d pointer) {
		pointer.set(relativePosition);
	}

	public final void getWorldPosition(Vector3d pointer) {
		pointer.zero();

		GameObject last = this;
		while (last != null) {
			pointer.add(last.relativePosition);
			last = last.parent;
		}
	}

	public final double getRelativeX() {
		return relativePosition.x;
	}

	public final double getRelativeY() {
		return relativePosition.y;
	}

	public final double getRelativeZ() {
		return relativePosition.z;
	}

	public final double getWorldX() {
		double x = 0;

		GameObject last = this;
		while (last != null) {
			x += last.relativePosition.x;
			last = last.parent;
		}
		return x;
	}

	public final double getWorldY() {
		double y = 0;

		GameObject last = this;
		while (last != null) {
			y += last.relativePosition.y;
			last = last.parent;
		}
		return y;
	}

	public final double getWorldZ() {
		double z = 0;

		GameObject last = this;
		while (last != null) {
			z += last.relativePosition.z;
			last = last.parent;
		}
		return z;
	}

	// Position setters

	public final void setRelativePosition(Vector3d vec) {
		setRelativePosition(vec.x, vec.y, vec.z);
	}

	public final void setRelativePosition(double x, double y, double z) {
		relativePosition.set(x, y, z);
	}

	public final void setWorldPosition(Vector3d vec) {
		setWorldPosition(vec.x, vec.y, vec.z);
	}

	public final void setWorldPosition(double x, double y, double z) {
		Vector3d vec3 = new Vector3d();
		getWorldPosition(vec3);

		double xdiff = vec3.x - x;
		double ydiff = vec3.y - y;
		double zdiff = vec3.z - z;

		setRelativePosition(relativePosition.x - xdiff, relativePosition.y - ydiff, relativePosition.z - zdiff);
	}

	public final void setRelativeX(double x) {
		setRelativePosition(x, relativePosition.y, relativePosition.z);
	}

	public final void setRelativeY(double y) {
		setRelativePosition(relativePosition.x, y, relativePosition.z);
	}

	public final void setRelativeZ(double z) {
		setRelativePosition(relativePosition.x, relativePosition.y, z);
	}

	public final void setWorldX(double worldx) {
		Vector3d vec3 = new Vector3d();
		getWorldPosition(vec3);
		setWorldPosition(worldx, vec3.y, vec3.z);
	}

	public final void setWorldY(double worldy) {
		Vector3d vec3 = new Vector3d();
		getWorldPosition(vec3);
		setWorldPosition(vec3.x, worldy, vec3.z);
	}

	public final void setWorldZ(double worldz) {
		Vector3d vec3 = new Vector3d();
		getWorldPosition(vec3);
		setWorldPosition(vec3.x, vec3.y, worldz);
	}

	// Scale getters

	public final void getRelativeScale(Vector3d pointer) {
		pointer.set(relativeScale);
	}

	public final void getWorldScale(Vector3d pointer) {
		pointer.set(1);

		GameObject last = this;
		while (last != null) {
			pointer.mul(last.relativeScale);
			last = last.parent;
		}
	}

	public final double getXScale() {
		return relativeScale.x;
	}

	public final double getYScale() {
		return relativeScale.y;
	}

	public final double getZScale() {
		return relativeScale.z;
	}

	public final double getWorldXScale() {
		double x = 0;

		GameObject last = this;
		while (last != null) {
			x *= last.relativeScale.x;
			last = last.parent;
		}
		return x;
	}

	public final double getWorldYScale() {
		double y = 0;

		GameObject last = this;
		while (last != null) {
			y *= last.relativeScale.y;
			last = last.parent;
		}
		return y;
	}

	public final double getWorldZScale() {
		double z = 0;

		GameObject last = this;
		while (last != null) {
			z *= last.relativeScale.z;
			last = last.parent;
		}
		return z;
	}

	// Scale setters

	public final void setRelativeScale(Vector3d vec) {
		setRelativeScale(vec.x, vec.y, vec.z);
	}

	public final void setRelativeScale(double x, double y, double z) {
		relativeScale.set(x, y, z);
	}

	public final void setWorldScale(Vector3d vec) {
		setWorldScale(vec.x, vec.y, vec.z);
	}

	public final void setWorldScale(double x, double y, double z) {
		Vector3d vec3 = new Vector3d();
		getWorldScale(vec3);

		double xdiff = vec3.x / x;
		double ydiff = vec3.y / y;
		double zdiff = vec3.z / z;

		setRelativeScale(relativeScale.x / xdiff, relativeScale.y / ydiff, relativeScale.z / zdiff);
	}

	public final void setXScale(double x) {
		setRelativeScale(x, relativeScale.y, relativeScale.z);
	}

	public final void setYScale(double y) {
		setRelativeScale(relativeScale.x, y, relativeScale.z);
	}

	public final void setZScale(double z) {
		setRelativeScale(relativeScale.x, relativeScale.y, z);
	}

	public final void setWorldXScale(double worldx) {
		Vector3d vec3 = new Vector3d();
		getWorldScale(vec3);
		setWorldScale(worldx, vec3.y, vec3.z);
	}

	public final void setWorldYScale(double worldy) {
		Vector3d vec3 = new Vector3d();
		getWorldScale(vec3);
		setWorldScale(vec3.x, worldy, vec3.z);
	}

	public final void setWorldZScale(double worldz) {
		Vector3d vec3 = new Vector3d();
		getWorldScale(vec3);
		setWorldScale(vec3.x, vec3.y, worldz);
	}

	// Rotation getters

	public final void getRelativeRotation(Vector3d pointer) {
		pointer.set(relativeRotation);
	}

	public final void getWorldRotation(Vector3d pointer) {
		pointer.zero();

		GameObject last = this;
		while (last != null) {
			pointer.add(last.relativeRotation);
			last = last.parent;
		}
	}

	public final double getYaw() {
		return relativeRotation.x;
	}

	public final double getPitch() {
		return relativeRotation.y;
	}

	public final double getRoll() {
		return relativeRotation.z;
	}

	public final double getWorldYaw() {
		double yaw = 0;

		GameObject last = this;
		while (last != null) {
			yaw += last.relativeRotation.x;
			last = last.parent;
		}
		return yaw;
	}

	public final double getWorldPitch() {
		double pitch = 0;

		GameObject last = this;
		while (last != null) {
			pitch += last.relativeRotation.y;
			last = last.parent;
		}
		return pitch;
	}

	public final double getWorldRoll() {
		double roll = 0;

		GameObject last = this;
		while (last != null) {
			roll += last.relativeRotation.z;
			last = last.parent;
		}
		return roll;
	}

	// Rotation setters

	public final void setRelativeRotation(Vector3d vec) {
		setRelativeRotation(vec.x, vec.y, vec.z);
	}

	public final void setRelativeRotation(double x, double y, double z) {
		relativeRotation.set(x, y, z);
	}

	public final void setWorldRotation(Vector3d vec) {
		setWorldRotation(vec.x, vec.y, vec.z);
	}

	public final void setWorldRotation(double x, double y, double z) {
		Vector3d vec3 = new Vector3d();
		getWorldRotation(vec3);

		double xdiff = vec3.x - x;
		double ydiff = vec3.y - y;
		double zdiff = vec3.z - z;

		setRelativeRotation(relativeRotation.x - xdiff, relativeRotation.y - ydiff, relativeRotation.z - zdiff);
	}

	public final void setYaw(double yaw) {
		setRelativeRotation(yaw, relativeRotation.y, relativeRotation.z);
	}

	public final void setPitch(double pitch) {
		setRelativeRotation(relativeRotation.x, pitch, relativeRotation.z);
	}

	public final void setRoll(double roll) {
		setRelativeRotation(relativeRotation.x, relativeRotation.y, roll);
	}

	public final void setWorldYaw(double worldyaw) {
		Vector3d vec3 = new Vector3d();
		getWorldRotation(vec3);
		setWorldRotation(worldyaw, vec3.y, vec3.z);
	}

	public final void setWorldPitch(double worldpitch) {
		Vector3d vec3 = new Vector3d();
		getWorldRotation(vec3);
		setWorldRotation(vec3.x, worldpitch, vec3.z);
	}

	public final void setWorldRoll(double worldroll) {
		Vector3d vec3 = new Vector3d();
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
	public final void update(double delta) {
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

	protected void serverUpdate(double delta) {
		// WARNING: NONE of the code in this method
		// should EVER try to interact with render code
		// or other objects that require an opngGL context
		// as it will trigger errors or, in the worst
		// scenario, a SIGSEGV signal (Segmentation fault)
		// shutting down the entire server
		// (Which might even be a dedicated server as a whole)
	}

	protected void clientUpdate(double delta) {
		// Here doing such things may still cause
		// exceptions or weird and hard to debug errors
		// so by design it is best not to include such
		// code in update methods
	}

	protected void commonUpdate(double delta) {
		// Happens on both server and client regardless
		// So follow all the warnings reported on the serverUpdate
		// method plus the ones on clientUpdate
	}

	@Override
	public void render(Matrix4d projection, double delta) {
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		buffer.putDouble(relativePosition.x);
		buffer.putDouble(relativePosition.y);
		buffer.putDouble(relativePosition.z);

		buffer.putDouble(relativeScale.x);
		buffer.putDouble(relativeScale.y);
		buffer.putDouble(relativeScale.z);

		buffer.putDouble(relativeRotation.x);
		buffer.putDouble(relativeRotation.y);
		buffer.putDouble(relativeRotation.z);
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		relativePosition.x = buffer.getDouble();
		relativePosition.y = buffer.getDouble();
		relativePosition.z = buffer.getDouble();

		relativeScale.x = buffer.getDouble();
		relativeScale.y = buffer.getDouble();
		relativeScale.z = buffer.getDouble();

		relativeRotation.x = buffer.getDouble();
		relativeRotation.y = buffer.getDouble();
		relativeRotation.z = buffer.getDouble();
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
