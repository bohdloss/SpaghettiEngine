package com.spaghetti.audio;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL10;

import com.spaghetti.utils.Utils;

public class StaticSound extends Sound {

	protected int id;
	protected int format;
	protected ByteBuffer data;
	protected int frequency;

	@Override
	public void setData(Object... objects) {
		this.format = (int) objects[0];
		this.data = (ByteBuffer) objects[1];
		this.frequency = (int) objects[2];

		if (format == 0) {
			format = AL10.AL_FORMAT_MONO8;
		}
		if (frequency == 0) {
			frequency = 44100;
		}
	}

	@Override
	protected void load0() {
		id = AL10.alGenBuffers();
		Utils.alError();
		AL10.alBufferData(id, format, data, frequency);
		Utils.alError();

		// Not needed, wait for GC to pick it up
		data = null;
	}

	@Override
	protected void unload0() {
		status.clear();
		data.clear();
		AL10.alDeleteBuffers(id);
		Utils.alError();
	}

	@Override
	public boolean isFilled() {
		return data != null;
	}

	@Override
	public void update(SoundSource source) {
		AL10.alSourcei(source.getSourceId(), AL10.AL_LOOPING, source.isSourceLooping() ? AL10.AL_TRUE : AL10.AL_FALSE);
		Utils.alError();
		int alstate = AL10.alGetSourcei(source.getSourceId(), AL10.AL_SOURCE_STATE);
		Utils.alError();
		if (alstate == AL10.AL_STOPPED) {
			source.stop();
		}
	}

	@Override
	public void stateUpdate(SoundSource source) {
		int state = status.get(source);
		switch (state) {
		case PLAYING:
			AL10.alSourcei(source.getSourceId(), AL10.AL_BUFFER, id);
			Utils.alError();
			AL10.alSourcePlay(source.getSourceId());
			Utils.alError();
			break;
		case PAUSED:
			AL10.alSourcePause(source.getSourceId());
			Utils.alError();
			break;
		case STOPPED:
			AL10.alSourcei(source.getSourceId(), AL10.AL_LOOPING, 0);
			Utils.alError();
			AL10.alSourceStop(source.getSourceId());
			Utils.alError();
			AL10.alSourcei(source.getSourceId(), AL10.AL_BUFFER, 0);
			Utils.alError();
			break;
		}
	}

	// Getters

	@Override
	public int getId() {
		return id;
	}

	@Override
	public int getFormat() {
		return format;
	}

	@Override
	public ByteBuffer getData() {
		return data;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

}
