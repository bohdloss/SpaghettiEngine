package com.spaghetti.world;

import com.spaghetti.core.Game;
import com.spaghetti.render.Renderable;
import com.spaghetti.networking.Replicable;
import com.spaghetti.input.Updatable;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.*;

public abstract class GameComponent implements Updatable, Renderable, Replicable {

	private final void setFlag(int flag, boolean value) {
		synchronized (flags_lock) {
			flags = HashUtil.bitAt(flags, flag, value);
		}
	}

	private final boolean getFlag(int flag) {
		synchronized (flags_lock) {
			return HashUtil.bitAt(flags, flag);
		}
	}

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
	private GameObject owner;
	private int id;

	public GameComponent() {
		this.id = IdProvider.newId(getGame());
		setFlag(REPLICATE, true);
		setFlag(VISIBLE, true);
		setFlag(AWAKE, true);
	}

	// Interfaces

	protected final void doBegin() {
		if (!getFlag(INITIALIZED)) {
			try {
				onBeginPlay();
			} catch (Throwable t) {
				Logger.error("onBeginPlay() Error:", t);
			}
			setFlag(INITIALIZED, true);
		}
	}

	protected final void doEnd() {
		if (getFlag(INITIALIZED)) {
			try {
				onEndPlay();
			} catch (Throwable t) {
				Logger.error("onEndPlay() Error:", t);
			}
			setFlag(INITIALIZED, false);
		}
	}

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	public final void render(Camera renderer, float delta) {
		if(!getFlag(VISIBLE)) {
			return;
		}

		// Gather render cache
		Transform transform = new Transform();
		owner.getWorldPosition(transform.position);
		owner.getWorldRotation(transform.rotation);
		owner.getWorldScale(transform.scale);

		render(renderer, delta, transform);
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
	}

	// All the warnings from GameObject's
	// update methods apply here as well
	@Override
	public final void update(float delta) {
		if(!getFlag(AWAKE)) {
			return;
		}

		commonUpdate(delta);
		if (getGame().isClient()) {
			clientUpdate(delta);
		} else {
			serverUpdate(delta);
		}
	}

	protected void clientUpdate(float delta) {
	}

	protected void serverUpdate(float delta) {
	}

	protected void commonUpdate(float delta) {
	}

	// Hierarchy methods

	public final synchronized void destroy() {
		if (isDestroyed()) {
			return;
		}
		if (owner != null) {
			owner.removeComponent(id);
		}
		try {
			onDestroy();
		} catch (Throwable t) {
			Logger.error("Error occurred in component", t);
		}
		owner = null;
		setFlag(DESTROYED, true);
	}

	// Getters and setters

	protected final boolean isDestroyed() {
		return getFlag(DESTROYED);
	}

	public final GameObject getOwner() {
		return owner;
	}

	public final Level getLevel() {
		return owner == null ? null : owner.getLevel();
	}

	public final Game getGame() {
		return owner == null ? Game.getInstance() : owner.getGame();
	}

	public final int getId() {
		return id;
	}

	public final boolean isLocallyAttached() {
		return getFlag(ATTACHED);
	}

	public final boolean isGloballyAttached() {
		return owner != null && owner.isGloballyAttached();
	}

	public final boolean isInitialized() {
		return getFlag(INITIALIZED);
	}

	public final boolean isVisible() {
		return getFlag(VISIBLE);
	}

	public final void setVisible(boolean visible) {
		setFlag(VISIBLE, visible);
	}

	public final boolean isAwake() {
		return getFlag(AWAKE);
	}

	public final void setAwake(boolean awake) {
		setFlag(AWAKE, awake);
	}

	// Override for more precise control
	@Override
	public boolean needsReplication(ConnectionManager connection) {
		boolean flag = getFlag(REPLICATE);
		setFlag(REPLICATE, false);
		return flag;
	}

	protected final void setReplicateFlag(boolean flag) {
		setFlag(ATTACHED, flag);
	}

}
