package com.spaghetti.audio;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;
import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.utils.Utils;

public class SoundBuffer extends Asset {

	public static SoundBuffer get(String name) {
		return Game.getGame().getAssetManager().soundBuffer(name);
	}

	public static SoundBuffer require(String name) {
		return Game.getGame().getAssetManager().requireSoundBuffer(name);
	}

	protected int id;
	protected int format;
	protected ByteBuffer data;
	protected int frequency;

	@Override
	protected void load0() {
		id = AL10.alGenBuffers();
		Utils.alError();
		AL10.alBufferData(id, format, data, frequency);
		Utils.alError();

		// Let GC pick up the buffer
		data = null;
	}

	@Override
	public void setData(Object... objects) {
		format = (int) objects[0];
		data = (ByteBuffer) objects[1];
		frequency = (int) objects[2];
	}

	@Override
	protected void delete0() {
		AL10.alDeleteBuffers(id);
		Utils.alError();
	}

	@Override
	public boolean isFilled() {
		return data != null;
	}

	@Override
	protected void reset0() {
		id = 0;
		format = 0;
		data = null;
		frequency = 0;
	}

	// Getters

	public int getId() {
		return id;
	}

}
