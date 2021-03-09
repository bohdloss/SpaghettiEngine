package com.spaghetti.events;

import java.util.HashMap;

import com.spaghetti.core.Game;

public abstract class GameEvent {

	private static final HashMap<Integer, Long> staticId = new HashMap<>();

	public static final int BLACKLIST = -1;
	public static final int NONE = 0;
	public static final int WHITELIST = 1;

	public static final int CLIENT = -1;
	public static final int NOT_SET = 0;
	public static final int SERVER = 1;

	protected final long id;
	protected int maskType;
	protected int from;
	protected boolean cancelled;
	protected long[] mask;

	public GameEvent() {
		int index = Game.getGame().getIndex();
		Long get = staticId.get(index);
		if (get == null) {
			get = 0l;
			staticId.put(index, get);
		}
		id = get;

		initMask();
	}

	// Override this function to change default applied masks
	protected void initMask() {
		maskType = NONE;
		mask = new long[0];
	}

	// Getters and setters

	public int getMaskType() {
		return maskType;
	}

	public void setMaskType(int type) {
		this.maskType = type;
	}

	public boolean containsMask(long value) {
		for (long element : mask) {
			if (element == value) {
				return true;
			}
		}
		return false;
	}

	public int getMaskAmount() {
		return mask.length;
	}

	public long getMaskAt(int index) {
		return mask[index];
	}

	public void setMaskAt(int index, long value) {
		mask[index] = value;
	}

	public void addMask(long value) {
		if (containsMask(value)) {
			return;
		}
		long[] old = this.mask;
		mask = new long[mask.length + 1];
		for (int i = 0; i < mask.length - 1; i++) {
			mask[i] = old[i];
		}
		mask[mask.length - 1] = value;
	}

	public void removeMask(long value) {
		if (!containsMask(value)) {
			return;
		}
		long[] old = this.mask;
		mask = new long[mask.length - 1];
		int offset = 0;
		for (int i = 0; i < mask.length; i++) {
			if (old[i] == value) {
				offset = 1;
			}
			mask[i] = old[i + offset];
		}
	}

	public void setFrom(int from) {
		if (this.from == NOT_SET) {
			this.from = from;
		}
	}

	public int getFrom() {
		return from;
	}

	public long getId() {
		return id;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public boolean isCancelled() {
		return cancelled;
	}

}