package com.spaghetti.world;

import com.spaghetti.core.Game;
import com.spaghetti.render.Renderable;
import com.spaghetti.networking.Replicable;
import com.spaghetti.input.Updatable;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.*;

public abstract class GameComponent implements Updatable, Renderable, Replicable {

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

	public final void triggerDealloc() {
		if(!getGame().isHeadless() && getRenderCacheIndex() != -1) {
			getGame().getRenderer().deallocCache(getRenderCacheIndex());
			setRenderCacheIndex(-1);
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
		setRenderCacheIndex(-1);
		internal_setflag(REPLICATE, true);
		internal_setflag(VISIBLE, true);
		internal_setflag(AWAKE, true);
	}

	// Interfaces

	protected final void onbegin_check() {
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

	protected final void onend_check() {
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

	public final void render(Camera renderer, float delta) {
		if(!internal_getflag(VISIBLE)) {
			return;
		}

		// Gather render cache
		int cache_index = getRenderCacheIndex();
		Transform transform;
		if(cache_index == -1) {
			transform = new Transform();
			getOwner().getWorldPosition(transform.position);
			getOwner().getWorldRotation(transform.rotation);
			getOwner().getWorldScale(transform.scale);
		} else {
			transform = getGame().getRenderer().getTransformCache(cache_index);
		}

		render(renderer, delta, transform);
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
	}

	// All the warnings from GameObject's
	// update methods apply here as well
	@Override
	public final void update(float delta) {
		if(!internal_getflag(AWAKE)) {
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
		internal_setflag(DESTROYED, true);
	}

	// Getters and setters

	protected final boolean isDestroyed() {
		return internal_getflag(DESTROYED);
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
		return internal_getflag(ATTACHED);
	}

	public final boolean isGloballyAttached() {
		return owner != null && owner.isGloballyAttached();
	}

	public final boolean isInitialized() {
		return internal_getflag(INITIALIZED);
	}

	public final boolean isVisible() {
		return internal_getflag(VISIBLE);
	}

	public final void setVisible(boolean visible) {
		internal_setflag(VISIBLE, visible);
		if(isInitialized() && visible) {
			triggerAlloc();
		} else {
			triggerDealloc();
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
		internal_setflag(ATTACHED, flag);
	}

}
