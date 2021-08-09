package com.spaghetti.core;

import java.util.HashMap;
import java.util.Random;

import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.networking.NetworkConnection;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class GameComponent implements Updatable, Renderable, Replicable {

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
	// 4 is initialized flag
	public static final int INITIALIZED = 4;

	private final Object flags_lock = new Object();
	private int flags;
	private GameObject owner;
	private int id;

	public GameComponent() {
		this.id = newId();

	}

	// Interfaces

	protected final void onbegin_check() {
		if(!internal_getflag(INITIALIZED)) {
			onBeginPlay();
			internal_setflag(INITIALIZED, true);
		}
	}
	
	protected final void onend_check() {
		if(internal_getflag(INITIALIZED)) {
			onEndPlay();
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
	public void writeDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
	}

	@Override
	public void render(Camera renderer, float delta) {
	}
	
	// All of the warnings from GameObject's
	// update methods apply here as well
	@Override
	public final void update(float delta) {
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
		return owner == null ? Game.getGame() : owner.getGame();
	}

	public final int getId() {
		return id;
	}

	public final boolean isLocallyAttached() {
		return Utils.bitAt(flags, ATTACHED);
	}

	public final boolean isGloballyAttached() {
		return owner != null && owner.isGloballyAttached();
	}

	public final boolean isInitialized() {
		return internal_getflag(INITIALIZED);
	}
	
	// Override for more precise control
	@Override
	public boolean needsReplication(NetworkConnection connection) {
		boolean flag = internal_getflag(REPLICATE);
		internal_setflag(REPLICATE, false);
		return flag;
	}

	protected final void setReplicateFlag(boolean flag) {
		internal_setflag(ATTACHED, flag);
	}

}
