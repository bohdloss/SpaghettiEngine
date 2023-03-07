package com.spaghetti.audio;

import java.nio.ByteBuffer;
import java.util.HashMap;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.render.Material;

public abstract class Sound extends Asset {

	public static Sound get(String name) {
		return Game.getInstance().getAssetManager().getAndLazyLoadAsset(name);
	}

	public static Sound require(String name) {
		return Game.getInstance().getAssetManager().getAndInstantlyLoadAsset(name);
	}

	public static final int DETACHED = -1, STOPPED = 0, PLAYING = 1, PAUSED = 2;

	protected HashMap<SoundSource, Integer> status = new HashMap<>();
	protected HashMap<SoundSource, Object> data = new HashMap<>();

	public abstract void update(SoundSource source);

	protected abstract void stateUpdate(SoundSource source);

	public void play(SoundSource source) {
		if (!status.containsKey(source)) {
			status.put(source, DETACHED);
			data.put(source, null);
		}
		int past = status.get(source);
		status.put(source, PLAYING);
		if (past != PLAYING) {
			stateUpdate(source);
		}
	}

	public void pause(SoundSource source) {
		if (!status.containsKey(source)) {
			status.put(source, DETACHED);
			data.put(source, null);
		}
		int past = status.get(source);
		status.put(source, PAUSED);
		if (past != PAUSED) {
			stateUpdate(source);
		}
	}

	public void stop(SoundSource source) {
		if (!status.containsKey(source)) {
			status.put(source, DETACHED);
			data.put(source, null);
		}
		int past = status.get(source);
		status.put(source, STOPPED);
		if (past != STOPPED) {
			stateUpdate(source);
		}
	}

	// Getters

	public abstract int getId();

	public abstract int getFormat();

	public abstract ByteBuffer getData();

	public abstract int getFrequency();

}