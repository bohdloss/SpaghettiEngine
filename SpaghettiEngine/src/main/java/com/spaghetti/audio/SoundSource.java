package com.spaghetti.audio;

import org.joml.Vector3f;
import org.lwjgl.openal.*;

import com.spaghetti.core.GameObject;
import com.spaghetti.interfaces.ToClient;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.*;

@ToClient
public class SoundSource extends GameObject {

	public static final int IDLE = 0, PLAYING = 1, PAUSED = 2;

	// Internal data
	protected int id;
	protected Sound sound;
	protected int status;
	protected Vector3f lastpos = new Vector3f();
	protected Vector3f lastcamera = new Vector3f();

	// Variables
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
		if (getGame().isHeadless()) {
			return;
		}
		getGame().getRendererDispatcher().quickQueue(() -> {
			if (id != 0) {
				AL11.alGetError();
				AL10.alSourceStop(id);
				Utils.alError();
				AL10.alSourcei(id, AL10.AL_LOOPING, 0);
				Utils.alError();
				if(sound != null) {
					sound.stop(this);
				}
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
		Logger.info("destroy");
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		super.writeDataServer(buffer);
		buffer.putString(true, sound == null ? "" : sound.getName(), NetworkBuffer.UTF_8);
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		super.readDataClient(buffer);
		String name = buffer.getString(true, NetworkBuffer.UTF_8);
		setSound(name.equals("") ? null : getGame().getAssetManager().sound(name));
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
	public void render(Camera renderer, float delta) {
		if(sound == null || !sound.valid()) {
			return;
		}
		if (id == 0) {
			id = AL10.alGenSources();
			Utils.alError();
		}
		
		// Update position and velocity
		Vector3f currentpos = new Vector3f();
		getWorldPosition(currentpos);
		Vector3f currentvel = new Vector3f();
		currentpos.sub(lastpos, currentvel);
		currentvel.div(delta / 1000);
		lastpos.set(currentpos);
		AL10.alSource3f(id, AL10.AL_POSITION, currentpos.x, currentpos.y, currentpos.z);
		if(delta != 0) { // Avoid getting Infinity or NaN as velocity
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
			if(destroyOnStop) {
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
		if(this.sound != null) {
			if(!getGame().isHeadless()) {
				getGame().getRendererDispatcher().quickQueue(() -> {
					sound.stop(this);
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