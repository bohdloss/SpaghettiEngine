package com.spaghettiengine.core;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghettiengine.interfaces.*;
import com.spaghettiengine.utils.SpaghettiBuffer;

public abstract class GameObject implements Tickable, Renderable, Replicable {

	// Hierarchy and utility

	private static HashMap<Integer, Long> staticId = new HashMap<>();

	private static final void setComponentOwner(GameComponent gc, GameObject go) {
		try {
			Field field = GameComponent.class.getDeclaredField("owner");
			field.setAccessible(true);
			field.set(gc, go);
		} catch(Throwable t) {
			// Not going to happen
			t.printStackTrace();
		}
	}
	
	private static final void rebuildLevel(GameObject caller, GameObject child) {
		if(child.hierarchy == 0) {
			if(child.level != null) {
				child.level.removeObject(child.id);
			}
		} else {
			if(child.parent != null) {
				child.parent.removeChild(child.id);
			}
		}
		child.level = caller.level;
		child.children.forEach((id, childObject) -> {
			rebuildLevel(child, childObject);
		});
	}
	
	private static final void rebuildHierarchy(GameObject caller, GameObject child) {

		child.parent = caller;
		child.hierarchy = caller.hierarchy + 1;

		child.children.forEach((id, childObject) -> {
			rebuildHierarchy(child, childObject);
		});

	}
	
	private static final void rebuildObject(GameObject caller, GameObject child) {
		rebuildLevel(caller, child);
		rebuildHierarchy(caller, child);
	}
	
	// Instance methods and fields

	private boolean destroyed;
	private int hierarchy; // This identifies how deep this object is in the hierarchy
	private final long id; // This uniquely identifies any object
	private Level level;
	private GameObject parent;
	private LinkedHashMap<Long, GameObject> children = new LinkedHashMap<>();
	private LinkedList<GameComponent> components = new LinkedList<>();

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
		} else {
			parent.addChild(this);
		}
		level.ordered.put(this.id, this);
	}
	
	// Add objects or components
	
	public final synchronized void addChild(GameObject object) {
		if(isDestroyed() || object == null || object.isDestroyed()) {
			return;
		}
		
		if (object.parent == this) {
			// This is already a child of this object
			return;
		}
		if (object == this) {
			// This is us
			return;
		}
		
		rebuildObject(this, object);
		children.put(object.id, object);
		
		object.onBeginPlay();
	}

	public final synchronized void addComponent(GameComponent component) {
		if(component == null || component.isDestroyed()) {
			return;
		}
		
		setComponentOwner(component, this);
		components.add(component);
		
		component.onBeginPlay();
	}
	
	// Get objects and components

	public final synchronized GameObject getChild(long id) {
		return children.get(id);
	}

	public final synchronized GameObject getChildIndex(int index) {
		int i = 0;
		for(GameObject object : children.values()) {
			if(i == index) {
				return object;
			}
			i++;
		}
		throw new IndexOutOfBoundsException(""+index);
	}
	
	private GameObject _obj;
	private GameComponent _com;
	private int i;

	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T getChild(Class<T> cls) {
		_obj = null;
		children.forEach((id, object) -> {
			if (object.getClass().equals(cls)) {
				_obj = object;
			}
		});
		if (_obj == null) {
			return null;
		}
		return (T) _obj;
	}

	public final synchronized GameObject getChildN(Class<? extends GameObject> cls) {
		_obj = null;
		children.forEach((id, object) -> {
			if (object.getClass().equals(cls)) {
				_obj = object;
			}
		});
		return _obj;
	}
	
	public final synchronized int getChildrenAmount(Class<? extends GameObject> cls) {
		i = 0;
		children.forEach((id, object) -> {
			if(object.getClass().equals(cls)) {
				i++;
			}
		});
		return i;
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameObject> T[] getChildren(Class<T> cls, T[] buffer) {
		i = 0;
		children.forEach((id, object) -> {
			if(object.getClass().equals(cls)) {
				buffer[i] = (T) object;
				i++;
			}
		});
		return buffer;
	}
	
	public final synchronized GameObject[] getChildrenN(Class<? extends GameObject> cls, GameObject[] buffer) {
		i = 0;
		children.forEach((id, object) -> {
			if(object.getClass().equals(cls)) {
				buffer[i] = object;
				i++;
			}
		});
		return buffer;
	}
	
	public final synchronized int getComponentIndex(GameComponent component) {
		i = 0;
		for(GameComponent comp : components) {
			if(comp == component) {
				return i;
			}
			i++;
		}
		return -1;
	}
	
	public final synchronized GameComponent getComponent(int index) {
		return components.get(index);
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameComponent> T getComponent(Class<T> cls) {
		_com = null;
		components.forEach(component -> {
			if(component.getClass().equals(cls)) {
				_com = component;
			}
		});
		return (T) _com;
	}
	
	public final synchronized GameComponent getComponentN(Class<? extends GameComponent> cls) {
		_com = null;
		components.forEach(component -> {
			if(component.getClass().equals(cls)) {
				_com = component;
			}
		});
		return _com;
	}
	
	public final synchronized int getComponentAmount(Class<? extends GameComponent> cls) {
		i = 0;
		components.forEach(component -> {
			if(component.getClass().equals(cls)) {
				i++;
			}
		});
		return i;
	}
	
	@SuppressWarnings("unchecked")
	public final synchronized <T extends GameComponent> T[] getComponents(Class<T> cls, T[] buffer) {
		i = 0;
		components.forEach(component -> {
			if(component.getClass().equals(cls)) {
				buffer[i] = (T) component;
			}
		});
		return buffer;
	}
	
	public final synchronized GameComponent[] getComponentsN(Class<? extends GameComponent> cls, GameComponent[] buffer) {
		i = 0;
		components.forEach(component -> {
			if(component.getClass().equals(cls)) {
				buffer[i] = component;
			}
		});
		return buffer;
	}
	
	// Remove objects or components

	public final synchronized void removeChild(long id) {
		GameObject removed = children.get(id);

		if (removed != null) {
			removed.onEndPlay();
			children.remove(id);
			removed.parent = null;
		}
	}

	public final synchronized void removeChildren() {
		children.forEach((id, child) -> {
			child.onEndPlay();
			child.parent = null;
		});
		children.clear();
	}
	
	public final synchronized void removeComponent(int index) {
		GameComponent removed = components.get(index);
		
		if(removed != null) {
			removed.onEndPlay();
			components.remove(index);
			setComponentOwner(removed, null);
		}
	}
	
	public final synchronized void removeComponents() {
		components.forEach(component -> {
			component.onEndPlay();
			setComponentOwner(component, null);
		});
		components.clear();
	}
	
	// Delete object or components

	public final synchronized void deleteChild(long id) {
		GameObject get = children.get(id);
		if (get != null) {
			get.destroy();
		}
	}

	@SuppressWarnings("unchecked")
	public final synchronized void deleteChildren() {
		Object[] entries = children.entrySet().toArray();
		
		for(Object e : entries) {
			Entry<Long, GameObject> entry = (Entry<Long, GameObject>) e;
			entry.getValue().destroy();
		}
	}
	
	public final synchronized void deleteComponent(int index) {
		components.get(index).destroy();
	}
	
	public final synchronized void deleteComponents() {
		Object[] entries = components.toArray();
		
		for(Object e : entries) {
			GameComponent entry = (GameComponent) e;
			entry.destroy();
		}
	}
	
	// Self destroy methods
	
	public synchronized final void destroy() {
		if(isDestroyed()) {
			return;
		}
		components.forEach(component -> {
			component.destroy();
		});
		if (parent != null) {
			parent.removeChild(id);
		}
		onDestroy();
		if (level != null) {
			level.removeObject(id);
		}
		hierarchy = -1;
		level = null;
		parent = null;
		deleteChildren();
		children = null;
		destroyed = true;
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

	// Cache
	protected Vector3d vec3Cache = new Vector3d();

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
		synchronized (vec3Cache) {
			getWorldPosition(vec3Cache);

			double xdiff = vec3Cache.x - x;
			double ydiff = vec3Cache.y - y;
			double zdiff = vec3Cache.z - z;

			relativePos.x -= xdiff;
			relativePos.y -= ydiff;
			relativePos.z -= zdiff;
		}
	}

	public final void setRelativeX(double x) {
		relativePos.x = x;
	}

	public final void setRelativeY(double y) {
		relativePos.y = y;
	}

	public final void setRelativeZ(double z) {
		relativePos.z = z;
	}

	public final void setWorldX(double worldx) {
		double x = getWorldX();
		double xdiff = x - worldx;

		relativePos.x -= xdiff;
	}

	public final void setWorldY(double worldy) {
		double y = getWorldY();
		double ydiff = y - worldy;

		relativePos.y -= ydiff;
	}

	public final void setWorldZ(double worldz) {
		double z = getWorldZ();
		double zdiff = z - worldz;

		relativePos.z -= zdiff;
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
		synchronized (vec3Cache) {
			getWorldScale(vec3Cache);

			double xdiff = vec3Cache.x / x;
			double ydiff = vec3Cache.y / y;
			double zdiff = vec3Cache.z / z;

			relativeScale.x /= xdiff;
			relativeScale.y /= ydiff;
			relativeScale.z /= zdiff;
		}
	}

	public final void setXScale(double x) {
		relativeScale.x = x;
	}

	public final void setYScale(double y) {
		relativeScale.y = y;
	}

	public final void setZScale(double z) {
		relativeScale.z = z;
	}

	public final void setWorldXScale(double worldx) {
		double x = getWorldXScale();
		double xdiff = x / worldx;

		relativeScale.x /= xdiff;
	}

	public final void setWorldYScale(double worldy) {
		double y = getWorldYScale();
		double ydiff = y / worldy;

		relativeScale.y /= ydiff;
	}

	public final void setWorldZScale(double worldz) {
		double z = getWorldZScale();
		double zdiff = z - worldz;

		relativeScale.z -= zdiff;
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
		synchronized (vec3Cache) {
			getWorldRotation(vec3Cache);

			double xdiff = vec3Cache.x - x;
			double ydiff = vec3Cache.y - y;
			double zdiff = vec3Cache.z - z;

			relativeRotation.x -= xdiff;
			relativeRotation.y -= ydiff;
			relativeRotation.z -= zdiff;
		}
	}

	public final void setYaw(double yaw) {
		relativeRotation.x = yaw;
	}

	public final void setPitch(double pitch) {
		relativeRotation.y = pitch;
	}

	public final void setRoll(double roll) {
		relativeRotation.z = roll;
	}

	public final void setWorldYaw(double worldyaw) {
		double x = getWorldYaw();
		double xdiff = x - worldyaw;

		relativeRotation.x -= xdiff;
	}

	public final void setWorldPitch(double worldpitch) {
		double y = getWorldPitch();
		double ydiff = y - worldpitch;

		relativeRotation.y -= ydiff;
	}

	public final void setWorldRoll(double worldroll) {
		double z = getWorldRoll();
		double zdiff = z - worldroll;

		relativeRotation.z -= zdiff;
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
		components.forEach(component -> {
			component.update(delta);
		});
		// TODO Detect if this is a server or client instance
		clientUpdate(delta);

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
	
	@Override
	public void render(Matrix4d projection, double delta) {
	}

	@Override
	public void getReplicateData(SpaghettiBuffer buffer) {
	}

	@Override
	public void setReplicateData(SpaghettiBuffer buffer) {
	}

}
