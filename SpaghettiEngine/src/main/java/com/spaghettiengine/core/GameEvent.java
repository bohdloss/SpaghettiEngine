package com.spaghettiengine.core;

public abstract class GameEvent {
	
	public static final int BLACKLIST = -1;
	public static final int NONE = 0;
	public static final int WHITELIST = 1;
	
	public static final int NOT_SET = -1;
	public static final int CLIENT = 0;
	public static final int SERVER = 1;
	
	protected int maskType = NONE;
	protected int from = NOT_SET;
	protected long[] mask;
	
	public GameEvent(long...ls) {
		initMask(ls);
	}
	
	protected void initDefaultMask(long[] possibleValues) {
		for(int i = 0; i < possibleValues.length; i++) {
			mask[i] = possibleValues[i];
		}
	}
	
	protected void initMask(long[] possibleValues) {
		mask = new long[possibleValues.length];
		initDefaultMask(possibleValues);
		// Custom code here
	}
	
	// Getters and setters
	
	public int getMaskType() {
		return maskType;
	}
	
	public void setMaskType(int type) {
		this.maskType = type;
	}
	
	public boolean containsMask(long value) {
		for(int i = 0; i < mask.length; i++) {
			if(mask[i] == value) {
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
		if(containsMask(value)) {
			return;
		}
		long[] old = this.mask;
		mask = new long[mask.length + 1];
		for(int i = 0; i < mask.length - 1; i++) {
			mask[i] = old[i];
		}
		mask[mask.length - 1] = value;
	}
	
	public void removeMask(long value) {
		if(!containsMask(value)) {
			return;
		}
		long[] old = this.mask;
		mask = new long[mask.length - 1];
		int offset = 0;
		for(int i = 0; i < mask.length; i++) {
			if(old[i] == value) {
				offset = 1;
			}
			mask[i] = old[i + offset];
		}
	}
	
	public void setFrom(int from) {
		if(this.from == NOT_SET) {
			this.from = from;
		}
	}
	
	public int getFrom() {
		return from;
	}
	
}