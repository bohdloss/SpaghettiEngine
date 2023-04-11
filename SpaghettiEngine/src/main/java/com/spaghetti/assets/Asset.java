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
	private boolean init;

	public final void load() {
		try {
			if (!init && isFilled()) {
				load0();
				init = true;
			}
		} catch (Throwable t) {
			throw new AssetException(this, "Error loading asset " + name, t);
		}
	}

	protected abstract void load0();

	public final void unload() {
		try {
			if (isLoaded()) {
				unload0();
			}
		} catch (Throwable t) {
			throw new AssetException(this, "Error unloading asset " + name, t);
		} finally {
			init = false;
		}
	}

	public abstract void setData(Object[] objects);

	protected abstract void unload0();

	public boolean isLoaded() {
		return init;
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