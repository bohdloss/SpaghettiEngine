package com.spaghetti.assets;

import com.spaghetti.core.Game;
import com.spaghetti.utils.Logger;

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
		try {
			if (!init && isFilled()) {
				load0();
				deleted = false;
				init = true;
			}
		} catch (Throwable t) {
			Logger.error("Error loading asset " + name, t);
		}
	}

	protected abstract void load0();

	public final void unload() {
		try {
			if (valid()) {
				unload0();
			}
		} catch (Throwable t) {
			Logger.error("Error unloading asset " + name, t);
		} finally {
			deleted = true;
			init = false;
		}
	}

	public abstract void setData(Object... objects);

	protected abstract void unload0();

	public final boolean isLoaded() {
		return init;
	}

	public final boolean isUnloaded() {
		return deleted;
	}

	public final void reload() {
		unload();
		load();
	}

	public boolean valid() {
		return !deleted && init;
	}

	public abstract boolean isFilled();

	public final void reset() {
		try {
			unload();
			reset0();
		} catch (Throwable t) {
			Logger.error("Error resetting asset " + name, t);
		}
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