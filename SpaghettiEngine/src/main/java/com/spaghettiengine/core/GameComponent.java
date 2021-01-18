package com.spaghettiengine.core;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghettiengine.interfaces.*;
import com.spaghettiengine.utils.SpaghettiBuffer;

public abstract class GameComponent implements Tickable, Renderable, Replicable, Cloneable {

	// Hierarchy and utility

	private static HashMap<Integer, Long> staticId = new HashMap<>();

	private static final void rebuildHierarchy(int iteration, GameComponent caller, GameComponent child) {

		if (caller.level != child.level) {
			if (child.level != null && child.hierarchy == 0) {
				child.level.removeComponent(child.id);
			} else if (child.hierarchy != 0 && iteration == 0) {
				if (child.parent != null) {
					child.parent.removeChild(child.id);
				}
			}
		}

		child.parent = caller;
		child.hierarchy = caller.hierarchy + 1;

		child.children.forEach((id, childComponent) -> {
			rebuildHierarchy(iteration + 1, child, childComponent);
		});

	}

	// Instance methods and fields

	private int hierarchy; // This identifies how deep this component is in the hierarchy
	private final long id; // This uniquely identifies any component
	private Level level;
	private GameComponent parent;
	private LinkedHashMap<Long, GameComponent> children = new LinkedHashMap<>();
	private Object[] entryset;
	private boolean rebuildListNeeded;

	public GameComponent(Level level, GameComponent parent) {
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

		if (parent == null) {
			level.components.add(this);
			hierarchy = 0;
		} else {
			parent.children.put(id, this);
		}
		level.ordered.put(id, this);

		rebuildListNeeded();
	}

	public final synchronized void addChild(GameComponent component) {
		if (component.parent == this) {
			return;
		}
		if (component == this) {
			return;
		}

		rebuildHierarchy(0, this, component);

		children.put(component.id, component);

		rebuildListNeeded();
	}

	private final void rebuildList() {
		if (rebuildListNeeded) {
			entryset = children.entrySet().toArray();
			rebuildListNeeded = false;
		}
	}

	private final void rebuildListNeeded() {
		rebuildListNeeded = true;
	}

	@SuppressWarnings("unchecked")
	public final synchronized GameComponent getChildIndex(int index) {
		rebuildList();
		return ((Entry<Long, GameComponent>) entryset[index]).getValue();
	}

	public final synchronized GameComponent getChild(long id) {
		return children.get(id);
	}

	private GameComponent _getChild_GameComponent_return;

	@SuppressWarnings("unchecked")
	public final synchronized <T> T getChild(Class<T> cls) {
		_getChild_GameComponent_return = null;
		children.forEach((id, component) -> {
			if (component.getClass().equals(cls)) {
				_getChild_GameComponent_return = component;
			}
		});
		if (_getChild_GameComponent_return == null) {
			return null;
		}
		return (T) _getChild_GameComponent_return;
	}

	public final synchronized GameComponent getChildN(Class<? extends GameComponent> cls) {
		_getChild_GameComponent_return = null;
		children.forEach((id, component) -> {
			if (component.getClass().equals(cls)) {
				_getChild_GameComponent_return = component;
			}
		});
		return _getChild_GameComponent_return;
	}

	// Remove just detaches a child component

	public final synchronized void removeChild(long id) {
		GameComponent removed = children.remove(id);

		if (removed != null) {
			removed.parent = null;
		}

		rebuildListNeeded();
	}

	// Delete does the same by calling destroy();

	public final synchronized void deleteChild(long id) {
		GameComponent get = children.get(id);
		if (get != null) {
			get.destroy();
		}
	}

	public final synchronized int getChildrenAmount() {
		return children.size();
	}

	public final GameComponent getParent() {
		return parent;
	}

	// Removes all children

	public final synchronized void removeChildren() {
		children.clear();
	}

	// Destroys all children

	public final synchronized void deleteChildren() {
		children.forEach((id, child) -> {
			child.destroy();
		});
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

	public synchronized final void destroy() {
		onDestroy();
		if (level != null) {
			level.removeComponent(id);
		}
		if (parent != null) {
			parent.removeChild(id);
		}
		hierarchy = -1;
		level = null;
		parent = null;
		deleteChildren();
		children = null;
	}

	protected void onDestroy() {
	}

	public final boolean isDestroyed() {
		return hierarchy == -1 || level == null || children == null;
	}

	public final synchronized GameComponent getBase() {
		if (hierarchy == 0) {
			return this;
		}
		if (hierarchy == 1) {
			return parent;
		}

		GameComponent last = parent;
		while (last.hierarchy != 0) {
			last = last.parent;
		}
		return last;
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

		GameComponent last = this;
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

		GameComponent last = this;
		while (last.hierarchy != 0) {
			x += last.relativePos.x;
			last = last.parent;
		}
		return x + last.relativePos.x;
	}

	public final double getWorldY() {
		double y = 0;

		GameComponent last = this;
		while (last.hierarchy != 0) {
			y += last.relativePos.y;
			last = last.parent;
		}
		return y + last.relativePos.y;
	}

	public final double getWorldZ() {
		double z = 0;

		GameComponent last = this;
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

		GameComponent last = this;
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

		GameComponent last = this;
		while (last.hierarchy != 0) {
			x *= last.relativeScale.x;
			last = last.parent;
		}
		return x * last.relativeScale.x;
	}

	public final double getWorldYScale() {
		double y = 0;

		GameComponent last = this;
		while (last.hierarchy != 0) {
			y *= last.relativeScale.y;
			last = last.parent;
		}
		return y * last.relativeScale.y;
	}

	public final double getWorldZScale() {
		double z = 0;

		GameComponent last = this;
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

		GameComponent last = this;
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

		GameComponent last = this;
		while (last.hierarchy != 0) {
			yaw += last.relativeRotation.x;
			last = last.parent;
		}
		return yaw + last.relativeRotation.x;
	}

	public final double getWorldPitch() {
		double pitch = 0;

		GameComponent last = this;
		while (last.hierarchy != 0) {
			pitch += last.relativeRotation.y;
			last = last.parent;
		}
		return pitch + last.relativeRotation.y;
	}

	public final double getWorldRoll() {
		double roll = 0;

		GameComponent last = this;
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

	@Override
	public final void update(double delta) {
		// TODO Detect if this is a server or client instance
		clientUpdate(delta);

		children.forEach((id, component) -> {
			component.update(delta);
		});
	}

	public abstract void serverUpdate(double delta);
	// WARNING: NONE of the code in this method
	// should EVER try to interact with render code
	// or other objects that require an opngGL context
	// as it will trigger errors or, in the worst
	// scenario, a SIGSEGV signal (Segmentation fault)
	// shutting down the entire server
	// (Which might even be a dedicated server as a whole)

	public abstract void clientUpdate(double delta);
	// Here doing such things may still cause
	// exceptions or weird and hard to debug errors
	// so by design it is best not to include such
	// code in update methods

	@Override
	public abstract void render(Matrix4d projection, double delta);

	@Override
	public void getReplicateData(SpaghettiBuffer buffer) {
	}

	@Override
	public void setReplicateData(SpaghettiBuffer buffer) {
	}

	@Override
	public final GameComponent clone() {
		try {
			@SuppressWarnings("rawtypes")
			Constructor constructor = this.getClass().getConstructor(Level.class, GameComponent.class);
			GameComponent clone = (GameComponent) constructor.newInstance(level, parent);

			SpaghettiBuffer buffer = new SpaghettiBuffer(1000);
			getReplicateData(buffer);
			buffer.getBuffer().position(0);
			clone.setReplicateData(buffer);

			return clone;

		} catch (Throwable t) {
			// Too many exceptions can happen, better catch 'em all
			t.printStackTrace();
		}
		return null;
	}

}
