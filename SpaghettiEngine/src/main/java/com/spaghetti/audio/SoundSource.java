package com.spaghetti.audio;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;
import com.spaghetti.core.GameObject;
import com.spaghetti.interfaces.ToClient;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.Utils;

@ToClient
public class SoundSource extends GameObject {

	public static final int IDLE = 0, PLAYING = 1, PAUSED = 2;

	// Internal data
	protected int id;
	protected SoundBuffer soundBuffer;
	protected int status;
	protected Vector3f lastpos = new Vector3f();
	protected boolean destroyOnStop;

	// Variables
	protected float gain;
	protected float pitch;
	protected boolean looping;

	public SoundSource() {
		gain = 1;
		pitch = 1;
	}

	public SoundSource(SoundBuffer soundBuffer) {
		this();
		this.soundBuffer = soundBuffer;
	}

	@Override
	protected void onBeginPlay() {
		if (getGame().isServer()) {
			getGame().getServer().queueWriteObjectTree(this);
			getGame().getServer().queueWriteObject(this);
		}
	}

	@Override
	protected void onEndPlay() {
		if (getGame().isHeadless()) {
			return;
		}
		getGame().getRendererDispatcher().queue(() -> {
			if (id != 0) {
				AL10.alSourceStop(id);
				Utils.alError();
				AL10.alSourcei(id, AL10.AL_LOOPING, 0);
				Utils.alError();
				AL10.alSourcei(id, AL10.AL_BUFFER, 0);
				Utils.alError();
				AL10.alDeleteSources(id);
				Utils.alError();
				id = 0;
			}
			return null;
		});
		if (getGame().isServer()) {
			getGame().getServer().queueWriteObjectDestruction(this);
		}
	}

	@Override
	protected void onDestroy() {
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		super.writeDataServer(buffer);
		buffer.putString(true, soundBuffer == null ? "" : soundBuffer.getName(), NetworkBuffer.UTF_8);
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		super.readDataClient(buffer);
		String name = buffer.getString(true, NetworkBuffer.UTF_8);
		soundBuffer = name.equals("") ? null : getGame().getAssetManager().soundBuffer(name);
	}

	public void play() {
		status = PLAYING;
	}

	public void pause() {
		status = PAUSED;
	}

	public void stop() {
		status = IDLE;
	}

	@Override
	public void render(Matrix4f projection, float delta) {
		if (id == 0) {
			id = AL10.alGenSources();
			Utils.alError();
			return;
		}
		// Update position and velocity
		Vector3f currentpos = new Vector3f();
		getWorldPosition(currentpos);
		Vector3f currentvel = new Vector3f();
		currentpos.sub(lastpos, currentvel);
		if (delta == 0) {
			currentvel.set(0);
		} else {
			currentvel.div(delta / 1000);
		}
		lastpos.set(currentpos);
		AL10.alSource3f(id, AL10.AL_POSITION, currentpos.x, currentpos.y, currentpos.z);
		AL10.alSource3f(id, AL10.AL_VELOCITY, currentvel.x, currentvel.y, currentvel.z);

		// Update properties
		AL10.alSourcef(id, AL10.AL_GAIN, gain);
		AL10.alSourcef(id, AL10.AL_PITCH, pitch);
		AL10.alSourcei(id, AL10.AL_LOOPING, looping ? 1 : 0);

		// Update stopped status
		if (AL10.alGetSourcei(id, AL10.AL_SOURCE_STATE) == AL10.AL_STOPPED) {
			status = IDLE;
			if (destroyOnStop) {
				destroy();
				return;
			}
		}

		// Update playing state
		switch (status) {
		case PLAYING:
			if (soundBuffer != null && soundBuffer.valid()
					&& AL10.alGetSourcei(id, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
				AL10.alSourcei(id, AL10.AL_BUFFER, soundBuffer.getId());
				Utils.alError();
				AL10.alSourcePlay(id);
				Utils.alError();
			}
			break;
		case PAUSED:
			AL10.alSourcePause(id);
			Utils.alError();
			break;
		case IDLE:
			AL10.alSourceStop(id);
			Utils.alError();
			break;
		}
	}

	// Getters and setters

	public int getSourceId() {
		return id;
	}

	public SoundBuffer getSoundBuffer() {
		return soundBuffer;
	}

	public void setSoundBuffer(SoundBuffer buffer) {
		this.soundBuffer = buffer;
	}

	public float getSourceGain() {
		return gain;
	}

	public void setSourceGain(float gain) {
		this.gain = gain;
	}

	public float getSourcePitch() {
		return pitch;
	}

	public void setSourcePitch(float pitch) {
		this.pitch = pitch;
	}

	public boolean isSourceLooping() {
		return looping;
	}

	public void setSourceLooping(boolean looping) {
		this.looping = looping;
	}

	public boolean destroyOnStop() {
		return destroyOnStop;
	}

	public void setDestroyOnStop(boolean destroyOnStop) {
		this.destroyOnStop = destroyOnStop;
	}

}