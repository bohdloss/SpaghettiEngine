package com.spaghetti.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC11;
import org.lwjgl.openal.ALUtil;

import com.spaghetti.utils.CMath;
import com.spaghetti.utils.Utils;

public class MicrophoneInputStream extends InputStream {

	public static final int MONO8 = AL11.AL_FORMAT_MONO8;
	public static final int STEREO8 = AL11.AL_FORMAT_STEREO8;

	public static final int DEFAULT_FORMAT = MONO8;
	public static final int DEFAULT_FREQUENCY = 44100;
	public static final int DEFAULT_TEMP_BUFFER_SIZE = 1024;
	public static final int DEFAULT_SECONDARY_BUFFER_SIZE = 1024;
	public static final int BPS = 8;
	public static final int DEFAULT_EMPTY_BIAS = StreamingSound.BUFFER_SIZE * StreamingSound.NUM_BUFFERS;
	
	public static String[] getCaptureDevices() {
		List<String> device_names = ALUtil.getStringList(0, ALC11.ALC_CAPTURE_DEVICE_SPECIFIER);
		String[] output = new String[device_names.size()];
		for(int i = 0; i < output.length; i++) {
			output[i] = device_names.get(i);
		}
		return output;
	}
	
	// Unused data
	protected String device_name;
	protected int format;
	protected int frequency;
	protected int temp_buffer_size;
	protected int empty_bias;
	
	// Used data
	protected long device_handle;
	protected ByteBuffer secondary_buffer;
	protected int remaining_bias;
	
	public MicrophoneInputStream(String deviceName, int format, int frequency, int tempBufferSize, int secondaryBufferSize, int emptyBias) {
		// Save arguments
		this.device_name = deviceName;
		this.format = format;
		this.frequency = frequency;
		this.temp_buffer_size = tempBufferSize;
		this.empty_bias = emptyBias;
		this.remaining_bias = emptyBias;
		
		// Check for valid format
		if(format != MONO8 && format != STEREO8) {
			throw new IllegalArgumentException("Only 8-bit formats are supported");
		}
		
		// Trying opening device
		device_handle = ALC11.alcCaptureOpenDevice(deviceName, frequency, format, tempBufferSize);
		Utils.alcError(device_handle);
		
		// Check if device failed to open
		if(device_handle == 0) {
			throw new IllegalArgumentException("Failed to open device");
		}
		
		// Start capturing audio
		ALC11.alcCaptureStart(device_handle);
		Utils.alcError(device_handle);
		
		// Initialize secondary buffer
		secondary_buffer = BufferUtils.createByteBuffer(secondaryBufferSize);
	}
	
	public MicrophoneInputStream() {
		this(null, DEFAULT_FORMAT, DEFAULT_FREQUENCY, DEFAULT_TEMP_BUFFER_SIZE, DEFAULT_SECONDARY_BUFFER_SIZE, DEFAULT_EMPTY_BIAS);
	}
	
	// Interface implementation
	
	@Override
	public int read() throws IOException {
		// Block until the sample is available
		while(available() == 0) {
			Utils.sleep(1);
		}
		
		if(remaining_bias > 0) {
			remaining_bias--;
			return 0;
		}
		
		// Capture exactly 1 sample
		secondary_buffer.clear();
		ALC11.alcCaptureSamples(device_handle, secondary_buffer, 1);
		Utils.alcError(device_handle);
		
		// Convert it to 0-255 value range
		return secondary_buffer.get(0) & 0xFF;
	}
	
	@Override
	public int read(byte[] array) throws IOException {
		// Quick sanity check to avoid NPE
		if(array == null) {
			return 0;
		}
		
		// Overload to other method
		return read(array, 0, array.length);
	}
	
	@Override
	public int read(byte[] array, int offset, int length) throws IOException {
		// Other sanity checks
		if(array == null || array.length == 0 || length == 0 || array.length < length + offset) {
			return 0;
		}
		secondary_buffer.clear();
		
		// Calculate maximum amount that can be captured
		int toCapture = (int) CMath.clampMax(available(), secondary_buffer.capacity());
		toCapture = (int) CMath.clampMax(toCapture, length);
		
		// We have some emptiness to put in the buffer
		if(remaining_bias > 0) {
			// Find maximum amount of empty we can put and decrease emptiness counter
			int copy_bias = (int) CMath.clampMax(remaining_bias, toCapture);
			remaining_bias -= copy_bias;
			
			// Put the emptiness
			for(int i = 0; i < copy_bias; i++) {
				secondary_buffer.put((byte) 0);
			}
			
			// If we have no more chance to put anything else just return immediately
			if(toCapture - copy_bias == 0) {
				// Copy to java array
				secondary_buffer.flip();
				secondary_buffer.get(array, offset, secondary_buffer.limit());
				
				return toCapture;
			}
			// Otherwise decrease toCapture counter and proceed capturing data
			toCapture -= copy_bias;
		}
		
		// Capture
		ALC11.alcCaptureSamples(device_handle, secondary_buffer, toCapture);
		Utils.alcError(device_handle);
		secondary_buffer.position(secondary_buffer.position() + toCapture);
		
		// Copy to java array
		secondary_buffer.flip();
		secondary_buffer.get(array, offset, secondary_buffer.limit());
		
		return toCapture;
	}
	
	@Override
	public long skip(long amount) throws IOException {
		long remaining = amount;
		
		// We may have some emptiness to skip
		if(remaining_bias > 0) {
			// Find maximum amount of empty we can put and decrease emptiness counter
			int ignore_bias = (int) CMath.clampMax(remaining_bias, amount);
			remaining_bias -= ignore_bias;
			
			// We used every chance to skip, just return immediately
			if(remaining - ignore_bias == 0) {
				return amount;
			}
			remaining -= ignore_bias;
		}
		
		// Keep capturing into buffer and ignore
		while(remaining > 0) {
			// Calculate maximum amount that can be captured
			int toCapture = (int) CMath.clampMax(available(), secondary_buffer.capacity());
			toCapture = (int) CMath.clampMax(toCapture, remaining);
			
			// Capture
			secondary_buffer.clear();
			ALC11.alcCaptureSamples(device_handle, secondary_buffer, toCapture);
			Utils.alcError(device_handle);
			
			// Decrease counter
			remaining -= toCapture;
		}
		return amount;
	}

	@Override
	public int available() throws IOException {
		int available = ALC11.alcGetInteger(device_handle, ALC11.ALC_CAPTURE_SAMPLES);
		Utils.alcError(device_handle);
		return available + remaining_bias;
	}
	
	@Override
	public void close() throws IOException {
		ALC11.alcCaptureStop(device_handle);
		Utils.alcError(device_handle);
		ALC11.alcCloseDevice(device_handle);
		Utils.alcError(device_handle);
	}
	
	@Override
	public void mark(int limit) {
	}
	
	@Override
	public void reset() throws IOException {
		throw new IOException("Mark / Reset not supported");
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	// Getters
	
	public String getDeviceName() {
		return device_name;
	}
	
	public int getFormat() {
		return format;
	}
	
	public int getFrequency() {
		return frequency;
	}
	
	public int getTempBufferSize() {
		return temp_buffer_size;
	}
	
	public int getSecondaryBufferSize() {
		return secondary_buffer.capacity();
	}
	
	public long getDeviceHandle() {
		return device_handle;
	}
	
}