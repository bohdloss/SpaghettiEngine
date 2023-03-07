package com.spaghetti.assets.loaders;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.spaghetti.assets.AssetLoader;
import com.spaghetti.utils.StreamUtil;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import com.spaghetti.audio.StaticSound;
import com.spaghetti.utils.ResourceLoader;

public class SoundLoader implements AssetLoader<StaticSound> {

	@Override
	public StaticSound instantiate() {
		return new StaticSound();
	}

	@Override
	public String[] getDependencies(String[] args) {
		return null;
	}

	@Override
	public Object[] load(String[] args) throws Throwable {
		AudioInputStream audio_stream = AudioSystem.getAudioInputStream(ResourceLoader.getStream(args[0]));
		AudioFormat audio_format = audio_stream.getFormat();

		// Read metadata
		int channels = audio_format.getChannels();
		int bps = audio_format.getSampleSizeInBits();
		int samplerate = (int) audio_format.getSampleRate();

		int format = AL10.AL_FORMAT_MONO8;
		if (channels == 1) {
			if (bps == 8) {
				format = AL10.AL_FORMAT_MONO8;
			} else if (bps == 16) {
				format = AL10.AL_FORMAT_MONO16;
			}
		} else if (channels == 2) {
			if (bps == 8) {
				format = AL10.AL_FORMAT_STEREO8;
			} else if (bps == 16) {
				format = AL10.AL_FORMAT_STEREO16;
			}
		}

		// Read audio data
		byte[] audio_data = new byte[audio_stream.available()];
		StreamUtil.effectiveRead(audio_stream, audio_data, 0, audio_data.length);
		ByteBuffer raw_data = ByteBuffer.wrap(audio_data);
		raw_data.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer dest_data = BufferUtils.createByteBuffer(audio_data.length);
		dest_data.order(ByteOrder.nativeOrder());

		// Reorder bytes
		int bytes = bps / 8;
		while (raw_data.hasRemaining()) {
			int pos = raw_data.position();
			for (int i = 0; i < bytes; i++) {
				dest_data.put(dest_data.order() == raw_data.order() ? raw_data.get(pos + i)
						: raw_data.get(pos + bytes - i - 1));
			}
			raw_data.position(pos + bytes);
		}
		dest_data.clear();

		return new Object[] { format, dest_data, samplerate };
	}

	@Override
	public String[] getDefaultArgs() {
		return new String[] {"/internal/error.wav"};
	}

}
