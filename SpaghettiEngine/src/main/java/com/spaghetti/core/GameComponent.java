package com.spaghetti.core;

import java.util.HashMap;
import java.util.Random;

import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class GameComponent implements Updatable, Replicable {

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
	private GameObject owner;
	private long id;

	public GameComponent() {
		this.id = newId();

	}

	// Interfaces

	protected void onBeginPlay() {
	}

	protected void onEndPlay() {
	}

	protected void onDestroy() {
	}

	@Override
	public void writeData(boolean isClient, NetworkBuffer buffer) {
	}

	@Override
	public void readData(boolean isClient, NetworkBuffer buffer) {
	}

	// All of the warnings from GameObject's
	// update methods apply here as well
	@Override
	public final void update(double delta) {
		commonUpdate(delta);

		if (getGame().isClient()) {
			clientUpdate(delta);
		} else {
			serverUpdate(delta);
		}
	}

	protected void clientUpdate(double delta) {
	}

	protected void serverUpdate(double delta) {
	}

	protected void commonUpdate(double delta) {
	}

	// Hierarchy methods

	public final void destroy() {
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
		return owner == null ? null : owner.getGame();
	}

	public final long getId() {
		return id;
	}

	public final boolean isLocallyAttached() {
		return Utils.bitAt(flags, ATTACHED);
	}

	public final boolean isGloballyAttached() {
		return owner != null && owner.isGloballyAttached();
	}

}
