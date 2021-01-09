package com.spaghettiengine.render;

import com.spaghettiengine.core.Game;

public abstract class RenderObject {

	public static RenderObject get(String name) {
		return Game.getGame().getAssetManager().custom(name);
	}
	
	public static RenderObject require(String name) {
		return Game.getGame().getAssetManager().requireCustom(name);
	}
	
	private boolean init, deleted, filled;

	public final void load() {
		if (!init && filled) {
			load0();
			deleted = false;
			init = true;
		}
	}

	protected abstract void load0();

	public final void delete() {
		if (valid()) {
			delete0();
			deleted = true;
			init = false;
		}
	}

	public void setData(Object...objects) {
	}
	
	protected abstract void delete0();

	public boolean isInitialized() {
		return init;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void reload() {
		delete();
		load();
	}

	public boolean valid() {
		return !deleted && init;
	}

	// Call on your custom setData() method!!!
	protected void setFilled(boolean filled) {
		this.filled = filled;
	}

	public boolean isFilled() {
		return filled;
	}
	
	public void reset() {
		delete();
		reset0();
		filled = true;
	}
	
	// Revert the effects of setData()
	protected abstract void reset0();
	
}