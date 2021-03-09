package com.spaghetti.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.function.BiConsumer;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;

public abstract class GameObject implements Tickable, Renderable, Replicable {

	// Hierarchy and utility

	private static final Field c_owner;
	private static HashMap<Integer, Long> staticId = new HashMap<>();

	static {
		Field f = null;
		try {
			f = GameComponent.class.getDeclaredField("owner");
			f.setAccessible(true);
		} catch (Throwable t) {
			// Will not happen
			t.printStackTrace();
		}
		c_owner = f;
	}

	private static final void setComponentOwner(GameComponent gc, GameObject go) {
		try {
			c_owner.set(gc, go);
		} catch (Throwable t) {
			// Will not happen
			t.printStackTrace();
		}
	}

	private static final void rebuildLevel(GameObject child) {

		if (child.hierarchy == 0) {
			// If this object has no parent remove it from the level directly
			child.level.removeObject(child.id);
			child.level.o_ordered.put(child.id, child);
		} else if (child.parent != null) {
			// Otherwise remove it from its parent
			child.parent.removeChild(child.id);
		}
		// The object will then be re-added with the appropriate function later

		// Repeat recursively
		child.children.forEach((id, childObject) -> {
			rebuildLevel(childObject);
		});
	}

	private static final void rebuildHierarchy(GameObject caller, GameObject child) {

		// Change parent of child
		child.parent = caller;
		// Change hierarchy level of child
		child.hierarchy = (caller == null ? -1 : caller.hierarchy) + 1;

		// Repeat recursively
		child.children.forEach((id, childObject) -> {
			rebuildHierarchy(child, childObject);
		});

	}

	public static final void rebuildObject(GameObject caller, GameObject child) {
		rebuildLevel(child);
		rebuildHierarchy(caller, child);
	}

	// Instance methods and m_fields

	private boolean destroyed;
	private int hierarchy; // This identifies how deep this object is in the hierarchy
	private long id; // This uniquely identifies any object
	private Level level;
	private GameObject parent;
	private HashMap<Long, GameObject> children = new HashMap<>();
	private HashMap<Long, GameComponent> components = new HashMap<>();

	public GameObject(Level level, GameObject parent) {
		if (level == null) {
			throw new IllegalArgumentException();
		}

		this.level = level;

		// Id calculation based on game instance

		int index = Game.getGame().getIndex();

		Long id = staticId.get(index);

		if (id == null) {
			id = 0l;
		}
		this.id = id;
		staticId.put(index, id + 1l);

		// Hierarchy initialization

		if (parent == null || parent.isDestroyed()) {
			level.objects.add(this);
			hierarchy = 0;
			level.o_ordered.put(this.id, this);
			_begin();
		} else {
			parent.addChild(this);
		}
	}

	// Utility

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
				// In this case, the object that needs to be added
				// is higher in the hierarchy than this one
				// but part of the same branch
				// this would create an infinite recursive operation
				// which results in StackOverflowException being thrown
				return;
			}
			current = current.parent;
		}

		// onEndPlay() happens unless object is a newly created instance
		rebuildObject(this, object);
		children.put(object.id, object);

		// onBeginPlay() happens regardless
		object._begin();
	}

	public final synchronized void addComponent(GameComponent component) {
		if (isDestroyed() || component == null || component.isDestroyed() || component.getOwner() == this) {
			return;
		}

		if (component.getOwner() != null) {
			// onEndPlay() might happen if this component already has a parent
			component.getOwner().removeComponent(component.getId());
		}
		setComponentOwner(component, this);
		components.put(component.getId(), component);
		level.c_ordered.put(component.getId(), component);

		// onBeginPlay() happens regardless
		component.onBeginPlay();
	}

	// Getter utility functions

	// Get single child by class

	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T getChild(Class<T> cls) {
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return (T) obj;
			}
		}
		return null;
	}

	public final synchronized GameObject getChildN(Class<? extends GameObject> cls) {
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				return obj;
			}
		}
		return null;
	}

	// Get single child by index

	public final synchronized GameObject getChild(int index) {
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
	public final synchronized <T extends GameObject> T getChild(int index, Class<T> cls) {
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

	public final synchronized GameObject getChild(long id) {
		return children.get(id);
	}

	// Get amount of objects by class

	public final synchronized int getChildrenAmount(Class<? extends GameObject> cls) {
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
	public final synchronized <T extends GameObject> T[] getChildren(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = (T) obj;
				i++;
			}
		}
		return buffer;
	}

	public final synchronized GameObject[] getChildrenN(Class<? extends GameObject> cls, GameObject[] buffer,
			int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			if (cls.isAssignableFrom(obj.getClass())) {
				buffer[i + offset] = obj;
				i++;
			}
		}
		return buffer;
	}

	public final synchronized GameObject[] getChildren(GameObject[] buffer, int offset) {
		int i = 0;
		for (GameObject obj : children.values()) {
			buffer[i + offset] = obj;
			i++;
		}
		return buffer;
	}

	// Get single component by class

	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameComponent> T getComponent(Class<T> cls) {
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				return (T) component;
			}
		}
		return null;
	}

	public final synchronized GameComponent getComponentN(Class<? extends GameComponent> cls) {
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				return component;
			}
		}
		return null;
	}

	// Get single component by index

	public final synchronized GameComponent getComponent(int index) {
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
	public final synchronized <T extends GameComponent> T getComponent(int index, Class<T> cls) {
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

	public final synchronized GameComponent getComponent(long id) {
		return components.get(id);
	}

	// Get amounts of components by class

	public final synchronized int getComponentAmount(Class<? extends GameComponent> cls) {
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
	public final synchronized <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer, int offset) {
		int i = 0;
		for (GameComponent component : components.values()) {
			if (cls.isAssignableFrom(component.getClass())) {
				buffer[i + offset] = (T) component;
				i++;
			}
		}
		return buffer;
	}

	public final synchronized GameComponent[] getComponentsN(Class<? extends GameComponent> cls, GameComponent[] buffer,
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

	public final synchronized GameComponent[] getComponents(GameComponent[] buffer, int offset) {
		int i = 0;
		for (GameComponent component : components.values()) {
			buffer[i + offset] = component;
			i++;
		}
		return buffer;
	}

	// Remove objects

	public final synchronized GameObject removeChild(long id) {
		GameObject removed = children.get(id);

		if (removed != null) {

			removed._end();
			removed.parent = null;
			children.remove(id);
			level.objects.remove(removed);
			level.o_ordered.remove(id);

			return removed;
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
		GameComponent removed = components.get(id);

		if (removed != null) {
			removed.onEndPlay();
			components.remove(id);
			level.c_ordered.remove(id);
			setComponentOwner(removed, null);

			return removed;
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
		_end();
		_destroy();
	}

	private final void destroySimple() {
		if (parent != null) {
			parent.children.remove(id);
		}
		if (level != null) {
			level.o_ordered.remove(id);
			level.objects.remove(this);
		}
		hierarchy = -1;
		level = null;
		parent = null;
		children = null;
		destroyed = true;
	}

	// Propagate onBeginPlay, onEndPlay and onDestroy to children

	protected final void _begin() {
		onBeginPlay();
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.onBeginPlay();
		}
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object._begin();
		}
	}

	protected final void _end() {
		for (Object obj : children.values().toArray()) {
			GameObject object = (GameObject) obj;
			object._end();
		}
		for (Object obj : children.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.onEndPlay();
		}
		onEndPlay();
	}

	protected final void _destroy() {
		for (Object obj : children.values().toArray()) {
			GameObject child = (GameObject) obj;
			child._destroy();
		}
		for (Object obj : components.values().toArray()) {
			GameComponent component = (GameComponent) obj;
			component.destroy();
		}
		onDestroy();
		destroySimple();
	}

	// Getters

	public final synchronized int getChildrenAmount() {
		return children.size();
	}

	public final synchronized int getComponentAmount() {
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

	public final synchronized GameObject getBase() {
		if (hierarchy == 0) {
			return this;
		}
		if (hierarchy == 1) {
			return parent;
		}

		GameObject last = parent;
		while (last.hierarchy != 0) {
			last = last.parent;
		}
		return last;
	}

	public final boolean isDestroyed() {
		return destroyed;
	}

	// World interaction

	@Replicate
	protected Vector3d relativePos = new Vector3d();
	@Replicate
	protected Vector3d relativeScale = new Vector3d(1, 1, 1);
	@Replicate
	protected Vector3d relativeRotation = new Vector3d();

	// Position getters

	public final void getRelativePosition(Vector3d pointer) {
		pointer.set(relativePos);
	}

	public final void getWorldPosition(Vector3d pointer) {
		pointer.zero();

		GameObject last = this;
		while (last.hierarchy != 0) {
			pointer.add(last.relativePos);
			last = last.parent;
		}
		pointer.add(last.relativePos);
	}

	public final double getRelativeX() {
		return relativePos.x;
	}

	public final double getRelativeY() {
		return relativePos.y;
	}

	public final double getRelativeZ() {
		return relativePos.z;
	}

	public final double getWorldX() {
		double x = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			x += last.relativePos.x;
			last = last.parent;
		}
		return x + last.relativePos.x;
	}

	public final double getWorldY() {
		double y = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			y += last.relativePos.y;
			last = last.parent;
		}
		return y + last.relativePos.y;
	}

	public final double getWorldZ() {
		double z = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			z += last.relativePos.z;
			last = last.parent;
		}
		return z + last.relativePos.z;
	}

	// Position setters

	public final void setRelativePosition(Vector3d vec) {
		setRelativePosition(vec.x, vec.y, vec.z);
	}

	public final void setRelativePosition(double x, double y, double z) {
		relativePos.set(x, y, z);
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

		setRelativePosition(relativePos.x - xdiff, relativePos.y - ydiff, relativePos.z - zdiff);
	}

	public final void setRelativeX(double x) {
		setRelativePosition(x, relativePos.y, relativePos.z);
	}

	public final void setRelativeY(double y) {
		setRelativePosition(relativePos.x, y, relativePos.z);
	}

	public final void setRelativeZ(double z) {
		setRelativePosition(relativePos.x, relativePos.y, z);
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
		while (last.hierarchy != 0) {
			pointer.mul(last.relativeScale);
			last = last.parent;
		}
		pointer.mul(last.relativeScale);
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
		while (last.hierarchy != 0) {
			x *= last.relativeScale.x;
			last = last.parent;
		}
		return x * last.relativeScale.x;
	}

	public final double getWorldYScale() {
		double y = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			y *= last.relativeScale.y;
			last = last.parent;
		}
		return y * last.relativeScale.y;
	}

	public final double getWorldZScale() {
		double z = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			z *= last.relativeScale.z;
			last = last.parent;
		}
		return z * last.relativeScale.z;
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
		while (last.hierarchy != 0) {
			pointer.add(last.relativeRotation);
			last = last.parent;
		}
		pointer.add(last.relativeRotation);
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
		while (last.hierarchy != 0) {
			yaw += last.relativeRotation.x;
			last = last.parent;
		}
		return yaw + last.relativeRotation.x;
	}

	public final double getWorldPitch() {
		double pitch = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			pitch += last.relativeRotation.y;
			last = last.parent;
		}
		return pitch + last.relativeRotation.y;
	}

	public final double getWorldRoll() {
		double roll = 0;

		GameObject last = this;
		while (last.hierarchy != 0) {
			roll += last.relativeRotation.z;
			last = last.parent;
		}
		return roll + last.relativeRotation.z;
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
			component.update(delta);
		});
		commonUpdate(delta);
		if (getGame().isClient()) {
			clientUpdate(delta);
		} else {
			serverUpdate(delta);
		}

		children.forEach((id, object) -> {
			object.update(delta);
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
	public void writeData(NetworkBuffer buffer) {
	}

	@Override
	public void readData(NetworkBuffer buffer) {
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
