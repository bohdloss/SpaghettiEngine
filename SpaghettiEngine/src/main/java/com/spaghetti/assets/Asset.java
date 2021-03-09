package com.spaghetti.assets;

import com.spaghetti.core.Game;

public abstract class Asset {

	public static Asset get(String name) {
		return Game.getGame().getAssetManager().custom(name);
	}

	public static Asset require(String name) {
		return Game.getGame().getAssetManager().requireCustom(name);
	}

	private String name;
	private boolean init, deleted;

	public final void load() {
		if (!init && isFilled()) {
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

	public abstract void setData(Object... objects);

	protected abstract void delete0();

	public final boolean isInitialized() {
		return init;
	}

	public final boolean isDeleted() {
		return deleted;
	}

	public final void reload() {
		delete();
		load();
	}

	public boolean valid() {
		return !deleted && init;
	}

	public abstract boolean isFilled();

	public final void reset() {
		delete();
		reset0();
	}

	// Revert the effects of setData()
	protected abstract void reset0();

	public final String getName() {
		return name;
	}

	public final void setName(String name) {
		if (this.name != null) {
			return;
		}
		this.name = name;
	}

}