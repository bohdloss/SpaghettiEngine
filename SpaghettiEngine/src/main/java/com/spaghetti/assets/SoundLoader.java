package com.spaghetti.assets;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;

import com.spaghetti.audio.StaticSound;
import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.utils.ResourceLoader;
import com.spaghetti.utils.Utils;

public class SoundLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new StaticSound();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return null;
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		AudioInputStream audio_stream = AudioSystem.getAudioInputStream(ResourceLoader.getStream(asset.args[0]));
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
		Utils.effectiveRead(audio_stream, audio_data, 0, audio_data.length);
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
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
