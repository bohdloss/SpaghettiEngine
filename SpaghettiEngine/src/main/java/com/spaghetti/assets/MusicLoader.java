package com.spaghetti.assets;

import java.io.BufferedInputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.openal.AL10;

import com.spaghetti.audio.StreamingSound;
import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.interfaces.StreamProvider;
import com.spaghetti.utils.ResourceLoader;

public class MusicLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new StreamingSound();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return null;
	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		AudioInputStream audio_stream = null;
		try {
			audio_stream = AudioSystem
					.getAudioInputStream(new BufferedInputStream(ResourceLoader.getStream(asset.args[0])));
			AudioFormat audio_format = audio_stream.getFormat();

			StreamProvider provider = () -> AudioSystem
					.getAudioInputStream(new BufferedInputStream(ResourceLoader.getStream(asset.args[0])));

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

			int numbuffers = 0;
			int buffersize = 0;

			// Optional argument numbuffers
			if (asset.args.length >= 2) {
				try {
					numbuffers = Integer.parseInt(asset.args[1]);
				} catch (NumberFormatException e) {
				}
			}

			// Optional argument buffersize
			if (asset.args.length >= 3) {
				try {
					buffersize = Integer.parseInt(asset.args[2]);
				} catch (NumberFormatException e) {
				}
			}

			return new Object[] { format, samplerate, bps, ByteOrder.LITTLE_ENDIAN, provider, numbuffers, buffersize };
		} finally {
			if (audio_stream != null) {
				audio_stream.close();
			}
		}
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		return null;
	}

}
