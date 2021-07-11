package com.spaghetti.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import com.spaghetti.interfaces.StreamProvider;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public class StreamingSound extends Sound {

	public static final int NUM_BUFFERS = 4;
	public static final int BUFFER_SIZE = 65536;

	protected int format;
	protected int frequency;
	protected int bps;
	protected ByteOrder byteOrder;
	protected StreamProvider provider;
	protected int numbuffers;
	protected int buffersize;

	@Override
	public void setData(Object... objects) {
		this.format = (int) objects[0];
		this.frequency = (int) objects[1];
		this.bps = (int) objects[2];
		this.byteOrder = (ByteOrder) objects[3];
		this.provider = (StreamProvider) objects[4];
		this.numbuffers = (int) objects[5];
		this.buffersize = (int) objects[6];

		if (format == 0) {
			format = AL10.AL_FORMAT_MONO8;
		}
		if (frequency == 0) {
			frequency = 44100;
		}
		if (bps == 0) {
			bps = 8;
		}
		if (byteOrder == null) {
			byteOrder = ByteOrder.nativeOrder();
		}
		if (numbuffers == 0) {
			numbuffers = NUM_BUFFERS;
		}
		if (buffersize == 0) {
			buffersize = BUFFER_SIZE;
		}
	}

	public void setData(int format, int frequency, int bps, ByteOrder byteOrder, StreamProvider provider,
			int numbuffers, int buffersize) {
		this.setData(new Object[] { format, frequency, bps, byteOrder, provider, numbuffers, buffersize });
	}

	// Per-source data
	protected static class StreamingStruct {

		public VolatileSound[] buffers;
		public int playIndex;
		public int loadIndex;
		public InputStream input;
		public StreamingSound inst;

		public boolean firstTime = true;
		public boolean finished;

		public StreamingStruct(StreamingSound inst) {
			this.inst = inst;
			buffers = new VolatileSound[inst.numbuffers];
			for (int i = 0; i < buffers.length; i++) {
				buffers[i] = new VolatileSound();
				buffers[i].setData(inst.format, BufferUtils.createByteBuffer(inst.buffersize), inst.frequency);
				buffers[i].load();
			}
		}

		// Cycle functions

		public void increasePlay() {
			playIndex++;
			if (playIndex >= buffers.length) {
				playIndex = 0;
			}
		}

		public void increaseLoad() {
			loadIndex++;
			if (loadIndex >= buffers.length) {
				loadIndex = 0;
			}
		}

		// Get a new stream
		public boolean resetStream() {
			try {
				input = inst.provider.provideStream();
				return true;
			} catch (Throwable e) {
				return false;
			}
		}

		// Close the stream and ignore errors
		public void closeStream() {
			try {
				input.close();
			} catch (Throwable e) {
			}
		}

		// Read a bit of data into the buffer
		public boolean readStream(VolatileSound buffer) {
			try {
				// Read some data
				ByteBuffer data = buffer.getData();
				data.clear();
				int read = Utils.effectiveRead(input, data, 0, data.capacity());

				// Reorder bytes
				if (data.order() != inst.byteOrder) {
					byte[] buf = new byte[inst.bps / 8];
					while (data.hasRemaining()) {
						for (int i = 0; i < buf.length; i++) {
							buf[i] = data.get(data.position() + i);
						}
						for (int i = 0; i < buf.length; i++) {
							data.put(buf[buf.length - i - 1]);
						}
					}
				}
				data.flip();
				return read == data.capacity() && read > 0;
			} catch (IOException t) {
				Logger.error("Error while buffering audio", t);
				return false;
			}
		}

		// Dispose of resources
		public void destroy() {
			for (VolatileSound sound : buffers) {
				sound.unload();
			}
			buffers = null;
			playIndex = 0;
			loadIndex = 0;
			closeStream();
			inst = null;
		}

	}

	@Override
	protected void load0() {

	}

	@Override
	protected void unload0() {
		// Dispose of all structures
		status.clear();
		for (Object obj : data.values()) {
			if (obj == null) {
				continue;
			}
			((StreamingStruct) obj).destroy();
		}
		data.clear();
	}

	@Override
	public boolean isFilled() {
		return provider != null;
	}

	protected StreamingStruct getStruct(SoundSource source) {
		Object _data = data.get(source);
		if (_data == null) {
			data.put(source, new StreamingStruct(this));
		}
		return (StreamingStruct) data.get(source);
	}

	protected void destroyStruct(SoundSource source) {
		StreamingStruct struct = getStruct(source);
		struct.destroy();
		data.put(source, null);
	}

	@Override
	public void update(SoundSource source) {
		StreamingStruct structure = getStruct(source);
		int state = status.get(source);

		// In case the source is playing keep streaming in audio data
		if (state == PLAYING && !structure.finished) {
			int processed = AL10.alGetSourcei(source.getSourceId(), AL10.AL_BUFFERS_PROCESSED);

			if (processed > 0) {
				// Unqueue processed buffers here
				for (int i = 0; i < processed; i++) {
					VolatileSound buffer = structure.buffers[structure.playIndex];
					AL10.alSourceUnqueueBuffers(source.getSourceId(), new int[] { buffer.getId() });
					Utils.alError();

					structure.increasePlay();
				}

				// Then load these buffers with new data and re-queue them
				for (int i = 0; i < processed; i++) {
					VolatileSound buffer = structure.buffers[structure.loadIndex];
					if (!structure.readStream(buffer)) {
						// We reached the end, raise finished flag
						structure.finished = true;
					}
					buffer.copyData();

					AL10.alSourceQueueBuffers(source.getSourceId(), buffer.getId());
					Utils.alError();

					structure.increaseLoad();
				}
			}
		}

		// Wait for the source to actually finish if the finished flag is on
		if (structure.finished) {
			int alstate = AL10.alGetSourcei(source.getSourceId(), AL10.AL_SOURCE_STATE);
			Utils.alError();

			if (alstate == AL10.AL_STOPPED) {

				// We reached the end, either loop or stop
				if (source.isSourceLooping()) {
					// Stop the source
					AL10.alSourceStop(source.getSourceId());
					Utils.alError();

					// Remove all buffers
					for (VolatileSound buffer : structure.buffers) {
						AL10.alSourceUnqueueBuffers(source.getSourceId(), new int[] { buffer.getId() });
						Utils.alError();
					}

					// Reset structure
					structure.finished = false;
					structure.firstTime = true;
					structure.playIndex = 0;
					structure.loadIndex = 0;
					structure.closeStream();

					// Force an update
					status.put(source, Sound.DETACHED);
					source.play();
				} else {
					source.stop();
				}
			}
		}
	}

	@Override
	protected void stateUpdate(SoundSource source) {
		// Get a structure to work with
		StreamingStruct structure = getStruct(source);

		// Update based on state
		int state = status.get(source);
		switch (state) {
		case PLAYING:
			// At first we initialize all buffers
			if (structure.firstTime) {
				// We need a fresh stream
				structure.resetStream();

				for (VolatileSound buffer : structure.buffers) {
					// Copy data into buffer
					structure.readStream(buffer);
					buffer.copyData();

					// Queue buffer
					AL10.alSourceQueueBuffers(source.getSourceId(), buffer.getId());
					Utils.alError();
				}

				structure.firstTime = false;
			}
			AL10.alSourcePlay(source.getSourceId());
			Utils.alError();
			break;
		case PAUSED:
			// Simply pause source
			AL10.alSourcePause(source.getSourceId());
			Utils.alError();
			break;
		case STOPPED:
			// Stop source to mark all buffers as queued
			AL10.alSourceStop(source.getSourceId());
			Utils.alError();

			// Unqueue all buffers
			for (VolatileSound buffer : structure.buffers) {
				AL10.alSourceUnqueueBuffers(source.getSourceId(), new int[] { buffer.getId() });
				Utils.alError();
			}

			// Destroy structure
			destroyStruct(source);
			break;
		}
	}

	// Getters

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public int getFormat() {
		return format;
	}

	@Override
	public ByteBuffer getData() {
		return null;
	}

	@Override
	public int getFrequency() {
		return frequency;
	}

	public int getBPS() {
		return bps;
	}

	public int getNumBuffers() {
		return numbuffers;
	}

	public int getBufferSize() {
		return buffersize;
	}

	public ByteOrder getByteOrder() {
		return byteOrder;
	}

	public StreamProvider getStreamProvider() {
		return provider;
	}

}