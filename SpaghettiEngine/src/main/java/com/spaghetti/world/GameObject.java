package com.spaghetti.world;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import com.spaghetti.core.Game;
import com.spaghetti.utils.*;
import org.joml.Vector3f;

import com.spaghetti.render.Renderable;
import com.spaghetti.networking.Replicable;
import com.spaghetti.input.Updatable;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.render.Camera;

public class GameObject implements Updatable, Renderable, Replicable {

	// Hierarchy and utility

	private static final Field c_owner = ReflectionUtil.getPrivateField(GameComponent.class, "owner");
	private static final Method c_setflag = ReflectionUtil.getPrivateMethod(GameComponent.class, "internal_setflag", int.class,
			boolean.class);

	// Instance methods and fields

	// O is attached flag
	public static final int ATTACHED = 0;
	// 1 is destroyed flag
	public static final int DESTROYED = 1;
	// 2 is delete flag
	public static final int DELETE = 2;
	// 3 is replicate flag
	public static final int REPLICATE = 3;
	// 4 is initialized flag
	public static final int INITIALIZED = 4;
	// 5 is visible flag
	public static final int VISIBLE = 5;
	// 6 is awake flag
	public static final int AWAKE = 6;
	// Last 16 bits are reserved for the render cache index

	private final Object flags_lock = new Object();
	private int flags;
	private int id; // This uniquely identifies any object
	private Level level;
	private GameObject parent;
	private ConcurrentHashMap<Integer, GameObject> children = new ConcurrentHashMap<>();
	private ConcurrentHashMap<Integer, GameComponent> components = new ConcurrentHashMap<>();

	public GameObject() {
		this.id = IdProvider.newId(getGame());
		setRenderCacheIndex(-1);
		internal_setflag(REPLICATE, true);
		internal_setflag(AWAKE, true);
		internal_setflag(VISIBLE, true);
	}

	// Utility

	private final void internal_setflag(int flag, boolean value) {
		synchronized (flags_lock) {
			flags = HashUtil.bitAt(flags, flag, value);
		}
	}

	private final boolean internal_getflag(int flag) {
		synchronized (flags_lock) {
			return HashUtil.bitAt(flags, flag);
		}
	}

	public final void triggerAlloc() {
		if(!getGame().isHeadless() && getRenderCacheIndex() == -1) {
			setRenderCacheIndex(getGame().getRenderer().allocCache());
		}
	}

	public final void triggerAllocRecursive() {
		if(!getGame().isHeadless()) {
			triggerAlloc();
			components.forEach((id, component) -> component.triggerAlloc());
			children.forEach((id, object) -> object.triggerAllocRecursive());
		}
	}

	public final void triggerDealloc() {
		if(!getGame().isHeadless() && getRenderCacheIndex() != -1) {
			getGame().getRenderer().deallocCache(getRenderCacheIndex());
			setRenderCacheIndex(-1);
		}
	}

	public final void triggerDeallocRecursive() {
		if(!getGame().isHeadless()) {
			triggerDealloc();
			components.forEach((id, component) -> component.triggerDealloc());
			children.forEach((id, object) -> object.triggerDeallocRecursive());
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
		internal_updatelevel(object);

		// Finally add the object, set flags and activate the object (onBeginPlay
		// triggers)
		object.parent = this;
		children.put(object.id, object);
		object.internal_setflag(ATTACHED, true);
		if (isGloballyAttached()) {
			object.onbegin_forward();
		}
	}

	private final void internal_updatelevel(GameObject object) {
		object.level = level;
		if (isGloballyAttached()) {
			level.o_ordered.put(object.id, object);
			object.components.forEach((id, component) -> {
				level.c_ordered.put(component.getId(), component);
			});
		}
		object.children.forEach((id, child) -> {
			internal_updatelevel(child);
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
				component.onbegin_check();
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
				object.onend_forward();
				// Remove from level
				level.o_ordered.remove(id);
			}

			// Remove from list, set flags
			object.parent = null;
			object.level = null;
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
					component.onend_check();
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
		ondestroy_forward();
	}

	private final void destroy_finalize() {
		if (parent == null) {
			if (level != null) {
				level.removeObject(id);
			}
		} else {
			parent.removeChild(id);
		}
		try {
			onDestroy();
		} catch (Throwable t) {
			Logger.error("Error occurred in object", t);
		}
		level = null;
		parent = null;
		internal_setflag(DESTROYED, true);
	}

	protected final void onbegin_forward() {
		onbegin_check();
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.onbegin_check();
		}
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object.onbegin_forward();
		}
	}

	protected final void onend_forward() {
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object.onend_check();
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.onend_check();
		}
		onend_check();
	}

	protected final void ondestroy_forward() {
		for (Object obj : children.values().toArray()) {
			GameObject child = (GameObject) obj;
			child.ondestroy_forward();
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.destroy();
		}
		destroy_finalize();
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
		return level == null ? Game.getInstance() : level.source;
	}

	/**
	 * Just like {@link #getGame()} but more likely to be optimized by the JIT compiler
	 * <p>
	 * Unlike the original function , it is undefined behaviour what happens if this function
	 * is called while the object is not attached to a level
	 *
	 * @return
	 */
	protected final Game getGameDirect() {
		return level.source;
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
		return level.isAttached();
	}

	public final boolean isInitialized() {
		return internal_getflag(INITIALIZED);
	}

	public final boolean isVisible() {
		return internal_getflag(VISIBLE);
	}

	public final void setVisible(boolean visible) {
		// Change flag and synchronize with cache
		internal_setflag(VISIBLE, visible);
		if(isInitialized() && visible) {
			triggerAlloc();

			// Force an update on components and children since this object is now visible
			components.forEach((id, component) -> component.setVisible(component.isVisible()));
			children.forEach((id, object) -> object.setVisible(object.isVisible()));
		} else {
			triggerDealloc();

			// Deallocate all components and children cache since this object is now invisible
			triggerDeallocRecursive();
		}

	}

	public final boolean isAwake() {
		return internal_getflag(AWAKE);
	}

	public final void setAwake(boolean awake) {
		internal_setflag(AWAKE, awake);
	}

	public final void setRenderCacheIndex(int index) {
		synchronized(flags_lock) {
			int mask = Integer.MAX_VALUE >> 16;
			flags &= mask;
			int write = index << 16;
			flags |= write;
		}
	}

	public final int getRenderCacheIndex() {
		synchronized(flags_lock) {
			return flags >> 16;
		}
	}

	// Override for more precise control
	@Override
	public boolean needsReplication(ConnectionManager connection) {
		boolean flag = internal_getflag(REPLICATE);
		internal_setflag(REPLICATE, false);
		return flag;
	}

	protected final void setReplicateFlag(boolean flag) {
		internal_setflag(REPLICATE, flag);
	}

	// World interaction

	protected final Vector3f relativePosition = new Vector3f();
	protected final Vector3f relativeScale = new Vector3f(1, 1, 1);
	protected final Vector3f relativeRotation = new Vector3f();

	// Transform getters and setters

	public final void getRelativeTransform(Transform buffer) {
		getRelativePosition(buffer.position);
		getRelativeRotation(buffer.rotation);
		getRelativeScale(buffer.scale);
	}

	public final Transform getRelativeTransform() {
		Transform transform = new Transform();
		getRelativeTransform(transform);
		return transform;
	}

	public final void getWorldTransform(Transform buffer) {
		getWorldPosition(buffer.position);
		getWorldRotation(buffer.rotation);
		getWorldScale(buffer.scale);
	}

	public final Transform getWorldTransform() {
		Transform transform = new Transform();
		getWorldTransform(transform);
		return transform;
	}

	// Position getters

	protected void computeWorldPosition(Vector3f relativePosition,
										Vector3f superPosition, Vector3f superRotation,
										Vector3f pointer) {
		pointer.zero();

		if(parent == null) {
			pointer.set(relativePosition);
		} else {
			float targetX = relativePosition.x;
			float targetY = relativePosition.y;

			float dist = MathUtil.distance(0, 0, relativePosition.x, relativePosition.y);
			float angle = MathUtil.lookAt(relativePosition.x, relativePosition.y);
			float targetAngle = angle + superRotation.z;

			targetX = (float) (dist * Math.cos(targetAngle));
			targetY = (float) (dist * Math.sin(targetAngle));

			pointer.set(targetX + superPosition.x, targetY + superPosition.y, superPosition.z + relativePosition.z);
		}
	}

	public final Vector3f getRelativePosition() {
		return new Vector3f().set(relativePosition);
	}

	public final void getRelativePosition(Vector3f pointer) {
		pointer.set(relativePosition);
	}

	public final Vector3f getWorldPosition() {
		Vector3f vec = new Vector3f();
		getWorldPosition(vec);
		return vec;
	}


	public final void getWorldPosition(Vector3f pointer) {
		pointer.zero();

		if(parent == null) {
			pointer.set(relativePosition);
		} else {
			Vector3f superPosition = parent.getWorldPosition();
			Vector3f superRotation = parent.getWorldRotation();

			float cosX = (float) Math.cos(superRotation.x);
			float sinX = (float) Math.sin(superRotation.x);

			float cosY = (float) Math.cos(superRotation.y);
			float sinY = (float) Math.sin(superRotation.y);

			float cosZ = (float) Math.cos(superRotation.z);
			float sinZ = (float) Math.sin(superRotation.z);

			float targetX = (relativePosition.x * cosZ - relativePosition.y * sinZ) * cosY;
			float targetY = (relativePosition.x * sinZ - relativePosition.y * cosZ) * cosX;
			float targetZ = (relativePosition.z * cosX - relativePosition.y * sinX) * sinY;

			pointer.set(targetX + superPosition.x, targetY + superPosition.y, targetZ + superPosition.z);
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
		return getWorldPosition().x;
	}

	public final float getWorldY() {
		return getWorldPosition().y;
	}

	public final float getWorldZ() {
		return getWorldPosition().z;
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
		Vector3f vec = new Vector3f();
		getWorldPosition(vec);

		float xdiff = vec.x - x;
		float ydiff = vec.y - y;
		float zdiff = vec.z - z;

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

	public final Vector3f getRelativeScale() {
		return new Vector3f().set(relativeScale);
	}

	public final void getRelativeScale(Vector3f pointer) {
		pointer.set(relativeScale);
	}

	public final Vector3f getWorldScale() {
		Vector3f vec = new Vector3f();
		getWorldScale(vec);
		return vec;
	}

	public void getWorldScale(Vector3f pointer) {
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
		return getWorldScale().x;
	}

	public final float getWorldYScale() {
		return getWorldScale().y;
	}

	public final float getWorldZScale() {
		return getWorldScale().z;
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

	public final Vector3f getRelativeRotation() {
		return new Vector3f().set(relativeRotation);
	}

	public final void getRelativeRotation(Vector3f pointer) {
		pointer.set(relativeRotation);
	}

	public final Vector3f getWorldRotation() {
		Vector3f vec = new Vector3f();
		getWorldRotation(vec);
		return vec;
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
		return getWorldRotation().x;
	}

	public final float getWorldPitch() {
		return getWorldRotation().y;
	}

	public final float getWorldRoll() {
		return getWorldRotation().z;
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

	private final void onbegin_check() {
		if (!internal_getflag(INITIALIZED)) {
			try {
				if(isVisible()) {
					triggerAlloc();
				}
				onBeginPlay();
			} catch (Throwable t) {
				Logger.error("onBeginPlay() Error:", t);
			}
			internal_setflag(INITIALIZED, true);
		}
	}

	private final void onend_check() {
		if (internal_getflag(INITIALIZED)) {
			try {
				triggerDealloc();
				onEndPlay();
			} catch (Throwable t) {
				Logger.error("onEndPlay() Error:", t);
			}
			internal_setflag(INITIALIZED, false);
		}
	}

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	@Override
	public final void update(float delta) {
		if(!internal_getflag(AWAKE)) {
			return;
		}

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

	/**
	 * WARNING: NONE of the code in this method should EVER try to interact with
	 * render code or other objects that require an opngGL context as it will
	 * trigger errors or, in the worst scenario, a SIGSEGV signal (Segmentation
	 * fault) shutting down the program along with all the sessions running in it (Which might even be a dedicated
	 * server as a whole)
	 *
	 * @param delta
	 */
	protected void serverUpdate(float delta) {

	}

	/**
	 * Here doing such things may still cause exceptions or weird and hard to debug
	 * errors so by design it is best not to include such code in update methods
	 *
	 * @param delta
	 */
	protected void clientUpdate(float delta) {

	}

	/**
	 * Happens on both server and client regardless So follow all the warnings
	 * reported on the serverUpdate method plus the ones on clientUpdate
	 *
	 * @param delta
	 */
	protected void commonUpdate(float delta) {
	}

	public final void render(Camera renderer, float delta) {
		if(!internal_getflag(VISIBLE)) {
			return;
		}

		components.forEach((id, component) -> {
			if (component != null) {
				component.render(renderer, delta);
			}
		});

		// Gather render cache
		int cache_index = getRenderCacheIndex();
		Transform transform;
		if(cache_index == -1) {
			transform = new Transform();
			getWorldPosition(transform.position);
			getWorldRotation(transform.rotation);
			getWorldScale(transform.scale);
		} else {
			Transform trans = getGame().getRenderer().getTransformCache(cache_index);
			Transform vel = getGame().getRenderer().getVelocityCache(cache_index);
			float velDelta = getGame().getRenderer().getCacheUpdateDelta();

			transform = new Transform();
			vel.position.mul(velDelta, transform.position);
			transform.position.add(trans.position);

			vel.rotation.mul(velDelta, transform.rotation);
			transform.rotation.add(trans.rotation);

			transform.scale.set(0);
			vel.scale.mul(velDelta, transform.scale);
			transform.scale.add(trans.scale);
		}

		if(this != renderer) {
			render(renderer, delta, transform);
		}
		children.forEach((id, object) -> {
			if (object != null) {
				object.render(renderer, delta);
			}
		});
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
	}

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		buffer.putFloat(relativePosition.x);
		buffer.putFloat(relativePosition.y);
		buffer.putFloat(relativePosition.z);

		if (relativeScale.x == 1 && relativeScale.y == 1 && relativeScale.z == 1) {
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
	public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		relativePosition.x = buffer.getFloat();
		relativePosition.y = buffer.getFloat();
		relativePosition.z = buffer.getFloat();

		if (buffer.getBoolean()) {
			relativeScale.x = 1;
			relativeScale.y = 1;
			relativeScale.z = 1;
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

}
