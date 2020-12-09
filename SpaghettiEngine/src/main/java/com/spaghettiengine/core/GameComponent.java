package com.spaghettiengine.core;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.joml.Matrix4d;
import org.joml.Vector2d;
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
			level.addComponent(this);
			hierarchy = 0;
		} else {
			parent.addChild(this);
		}

		rebuildListNeeded();
	}

	public final void addChild(GameComponent component) {
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
	public final GameComponent getChildIndex(int index) {
		rebuildList();
		return ((Entry<Long, GameComponent>) entryset[index]).getValue();
	}

	public final GameComponent getChild(long id) {
		return children.get(id);
	}

	// Remove just detaches a child component

	public final void removeChild(long id) {
		GameComponent removed = children.remove(id);

		if (removed != null) {
			removed.parent = null;
		}

		rebuildListNeeded();
	}

	// Delete does the same by calling destroy();

	public final void deleteChild(long id) {
		GameComponent get = children.get(id);
		if (get != null) {
			get.destroy();
		}
	}

	public final int getChildrenAmount() {
		return children.size();
	}

	public final GameComponent getParent() {
		return parent;
	}

	// Removes all children

	public final void removeChildren() {
		children.clear();
	}

	// Destroys all children

	public final void deleteChildren() {
		children.forEach((id, child) -> {
			child.destroy();
		});
	}

	public final Level getLevel() {
		return level;
	}

	public final Game getGame() {
		return level.source;
	}

	public final long getId() {
		return id;
	}

	public final void destroy() {
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

	@Override
	public void finalize() {
		destroy();
	}

	protected void onDestroy() {
	}

	public final boolean isDestroyed() {
		return hierarchy == -1 || level == null || children == null;
	}

	public final GameComponent getBase() {
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

	protected Vector3d relativePos = new Vector3d();
	protected Vector2d scale = new Vector2d(1, 1);
	protected double rotation;

	// Cache
	protected Vector2d vec2Cache = new Vector2d();
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

	// Position setters

	public void setRelativePosition(Vector3d vec) {
		relativePos.set(vec);
	}

	public void setWorldPosition(Vector3d vec) {
		synchronized (vec3Cache) {
			getWorldPosition(vec3Cache);

			double xdiff = vec3Cache.x - vec.x;
			double ydiff = vec3Cache.y - vec.y;
			double zdiff = vec3Cache.z - vec.z;

			relativePos.x -= xdiff;
			relativePos.y -= ydiff;
			relativePos.z -= zdiff;
		}
	}

	public void setRelativeX(double x) {
		relativePos.x = x;
	}

	public void setRelativeY(double y) {
		relativePos.y = y;
	}

	public void setRelativeZ(double z) {
		relativePos.z = z;
	}

	public void setWorldX(double worldx) {
		synchronized (vec3Cache) {
			getWorldPosition(vec3Cache);

			double xdiff = vec3Cache.x - worldx;

			relativePos.x -= xdiff;
		}
	}

	public void setWorldY(double worldy) {
		synchronized (vec3Cache) {
			getWorldPosition(vec3Cache);

			double ydiff = vec3Cache.y - worldy;

			relativePos.y -= ydiff;
		}
	}

	public void setWorldZ(double worldz) {
		synchronized (vec3Cache) {
			getWorldPosition(vec3Cache);

			double zdiff = vec3Cache.z - worldz;

			relativePos.z -= zdiff;
		}
	}

	// Scale getters

	public final void getScale(Vector2d pointer) {
		pointer.set(scale);
	}

	public final double getXScale() {
		return scale.x;
	}

	public final double getYScale() {
		return scale.y;
	}

	// Scale setters

	public void setScale(Vector2d vec) {
		scale.x = vec.x;
		scale.y = vec.y;
	}

	public void setXScale(double x) {
		scale.x = x;
	}

	public void setYScale(double y) {
		scale.y = y;
	}

	// Rotation getters

	public final double getRotation() {
		return rotation;
	}

	// Rotation setters

	public void setRotation(double rot) {
		this.rotation = rot;
	}

	// Physics

	protected boolean hasPhysics = false;
	protected Vector2d velocity = new Vector2d();
	protected Vector2d gravityMultiplier = new Vector2d(1, 1);
	protected Vector2d gravityOverride = new Vector2d();

	protected void physicsUpdate(float multiplier) {

	}

	// Interface methods

	protected Matrix4d matCache = new Matrix4d();

	@Override
	public final void update(float delta) {
		if (hasPhysics) {
			physicsUpdate(Game.getGame().getTickMultiplier(delta));
		}

		// TODO Detect if this is a server or client instance
		clientUpdate(delta);

		children.forEach((id, component) -> {
			component.update(delta);
		});
	}

	public void serverUpdate(float delta) {
		// WARNING: NONE of the code in this method
		// should EVER try to interact with render code
		// or other objects that require an opngGL context
		// as it will trigger errors or, in the worst
		// scenario, a SIGSEGV signal (Segmentation fault)
		// shutting down the entire server
		// (Which might even be a dedicated server as a whole)
	}

	public void clientUpdate(float delta) {
		// Here doing such things may still cause
		// exceptions or weird and hard to debug errors
		// so by design it is best not to include such
		// code in update methods
	}

	@Override
	public void render(Matrix4d projection) {
		matCache.set(projection);
		matCache.translate(relativePos).rotate(rotation, 0, 1, 0).scale(scale.x, scale.y, 1);
		renderUpdate();

		children.forEach((id, component) -> {
			component.render(matCache);
		});
	}

	public void renderUpdate() {
	}

	@Override
	public void getReplicateData(SpaghettiBuffer buffer) {

		// Default replication data

		buffer.putDouble(relativePos.x);
		buffer.putDouble(relativePos.y);
		buffer.putDouble(relativePos.z);

		buffer.putDouble(scale.x);
		buffer.putDouble(scale.y);

		buffer.putDouble(rotation);

	}

	@Override
	public void setReplicateData(SpaghettiBuffer buffer) {

		// Default set replication data

		relativePos.x = buffer.getDouble();
		relativePos.y = buffer.getDouble();
		relativePos.z = buffer.getDouble();

		scale.x = buffer.getDouble();
		scale.y = buffer.getDouble();

		rotation = buffer.getDouble();

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
