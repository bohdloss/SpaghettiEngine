package com.spaghetti.audio;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL11;

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
		
		if(format == 0) {
			format = AL11.AL_FORMAT_MONO8;
		}
		if(frequency == 0) {
			frequency = 44100;
		}
	}

	@Override
	protected void load0() {
		id = AL11.alGenBuffers();
		Utils.alError();
		AL11.alBufferData(id, format, data, frequency);
		Utils.alError();
		
		// Not needed, wait for GC to pick it up
		data = null;
	}
	
	@Override
	protected void unload0() {
		status.clear();
		data.clear();
		AL11.alDeleteBuffers(id);
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

	@Override
	public void update(SoundSource source) {
		AL11.alSourcei(source.getSourceId(), AL11.AL_LOOPING, source.isSourceLooping() ? AL11.AL_TRUE : AL11.AL_FALSE);
		Utils.alError();
		int alstate = AL11.alGetSourcei(source.getSourceId(), AL11.AL_SOURCE_STATE);
		Utils.alError();
		if(alstate == AL11.AL_STOPPED) {
			source.stop();
		}
	}
	
	@Override
	public void stateUpdate(SoundSource source) {
		int state = status.get(source);
		switch(state) {
		case PLAYING:
			AL11.alSourcei(source.getSourceId(), AL11.AL_BUFFER, id);
			Utils.alError();
			AL11.alSourcePlay(source.getSourceId());
			Utils.alError();
			break;
		case PAUSED:
			AL11.alSourcePause(source.getSourceId());
			Utils.alError();
			break;
		case STOPPED:
			AL11.alSourcei(source.getSourceId(), AL11.AL_LOOPING, 0);
			Utils.alError();
			AL11.alSourceStop(source.getSourceId());
			Utils.alError();
			AL11.alSourcei(source.getSourceId(), AL11.AL_BUFFER, 0);
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
