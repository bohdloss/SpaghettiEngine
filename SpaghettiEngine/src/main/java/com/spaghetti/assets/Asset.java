package com.spaghetti.assets;

import com.spaghetti.assets.exceptions.AssetException;
import com.spaghetti.core.Game;

public abstract class Asset {

	public static Asset get(String name) {
		return Game.getInstance().getAssetManager().getAndLazyLoadAsset(name);
	}

	public static Asset require(String name) {
		return Game.getInstance().getAssetManager().getAndInstantlyLoadAsset(name);
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
			throw new AssetException(this, "Error loading asset " + name, t);
		}
	}

	protected abstract void load0();

	public final void unload() {
		try {
			if (isValid()) {
				unload0();
			}
		} catch (Throwable t) {
			throw new AssetException(this, "Error unloading asset " + name, t);
		} finally {
			deleted = true;
			init = false;
		}
	}

	public abstract void setData(Object[] objects);

	protected abstract void unload0();

	public final boolean isLoaded() {
		return init;
	}

	public final boolean isUnloaded() {
		return deleted;
	}

	public boolean isValid() {
		return !deleted && init;
	}

	public abstract boolean isFilled();

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