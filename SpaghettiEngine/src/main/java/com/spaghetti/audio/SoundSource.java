package com.spaghetti.audio;

import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.utils.ExceptionUtil;
import org.joml.Vector3f;
import org.lwjgl.openal.AL10;

import com.spaghetti.world.GameObject;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.render.Camera;
import com.spaghetti.utils.Transform;

public class SoundSource extends GameObject {

	public static final int IDLE = 0, PLAYING = 1, PAUSED = 2;

	// Internal data
	protected int id;
	protected Sound sound;
	protected Vector3f lastpos = new Vector3f();
	protected Vector3f lastcamera = new Vector3f();

	// Variables
	protected int status;
	protected float gain;
	protected float pitch;
	protected boolean looping;
	protected boolean destroyOnStop;

	public SoundSource() {
		gain = 1;
		pitch = 1;
	}

	public SoundSource(Sound sound) {
		this();
		this.sound = sound;
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
		if (getGame().isServer()) {
			getGame().getServer().queueWriteObjectDestruction(this);
		}
		if (!getGame().isHeadless()) {
			getGame().getRendererDispatcher().quickQueue(() -> {
				if (id != 0) {
					AL10.alGetError();
					AL10.alSourceStop(id);
					ExceptionUtil.alError();
					AL10.alSourcei(id, AL10.AL_LOOPING, 0);
					ExceptionUtil.alError();
					if (sound != null) {
						sound.stop(this);
					}
					AL10.alDeleteSources(id);
					ExceptionUtil.alError();
					id = 0;
				}
				return null;
			});
		}
	}

	@Override
	protected void onDestroy() {
//		Logger.info("destroy");
	}

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		super.writeDataServer(manager, buffer);

		// Sound
		buffer.putString(true, sound == null ? "" : sound.getName(), NetworkBuffer.UTF_8);

		// Variables
		buffer.putByte((byte) status);
		buffer.putFloat(gain);
		buffer.putFloat(pitch);
		buffer.putBoolean(looping);
		buffer.putBoolean(destroyOnStop);
	}

	@Override
	public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		super.readDataClient(manager, buffer);

		// Sound
		String name = buffer.getString(true, NetworkBuffer.UTF_8);
		setSound(name.equals("") ? null : getGame().getAssetManager().getAndLazyLoadAsset(name));

		// Variables
		status = buffer.getByte() & 0xFF;
		gain = buffer.getFloat();
		pitch = buffer.getFloat();
		looping = buffer.getBoolean();
		destroyOnStop = buffer.getBoolean();
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
	public void render(Camera renderer, float delta, Transform transform) {
		if (sound == null || !sound.isLoaded()) {
			return;
		}
		if (id == 0) {
			id = AL10.alGenSources();
			ExceptionUtil.alError();
		}

		// Update position and velocity
		Vector3f currentpos = transform.position;
		Vector3f currentvel = new Vector3f();
		currentpos.sub(lastpos, currentvel);

		lastpos.set(currentpos);
		AL10.alSource3f(id, AL10.AL_POSITION, currentpos.x, currentpos.y, currentpos.z);
		if (delta != 0) { // Avoid getting Infinity or NaN as velocity
			currentvel.div(delta / 1000);
			AL10.alSource3f(id, AL10.AL_VELOCITY, currentvel.x, currentvel.y, currentvel.z);
		}

		// Update properties
		AL10.alSourcef(id, AL10.AL_GAIN, gain);
		AL10.alSourcef(id, AL10.AL_PITCH, pitch);

		// Update playing state
		switch (status) {
		case PLAYING:
			sound.play(this);
			break;
		case PAUSED:
			sound.pause(this);
			break;
		case IDLE:
			sound.stop(this);
			if (destroyOnStop) {
				destroy();
				return;
			}
			break;
		}

		// Update the sound then
		sound.update(this);
	}

	protected void internal_delete() {

	}

	// Getters and setters

	public int getSourceId() {
		return id;
	}

	public Sound getSound() {
		return sound;
	}

	public void setSound(Sound buffer) {
		if (this.sound == buffer) {
			return;
		}
		if (this.sound != null) {
			if (!getGame().isHeadless()) {
				getGame().getRendererDispatcher().quickQueue(() -> {
					this.sound.stop(this);
					return null;
				});
			}
		}
		this.sound = buffer;
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
		this.destroyOnStop = looping ? false : this.destroyOnStop;
	}

	public boolean destroyOnStop() {
		return destroyOnStop;
	}

	public void setDestroyOnStop(boolean destroyOnStop) {
		this.destroyOnStop = looping ? false : destroyOnStop;
	}

	public int getStatus() {
		return status;
	}

}