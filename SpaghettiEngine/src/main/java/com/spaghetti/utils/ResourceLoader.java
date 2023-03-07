package com.spaghetti.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

/**
 * ResourceLoader is an utility class that abstracts the loading of various file types
 * such as images, binary and text files into input streams or buffers / strings
 *
 * @author bohdloss
 *
 */
public final class ResourceLoader {

	private ResourceLoader() {
	}

	public static byte[] loadBinary(String location) throws IOException {
		return loadBinary(getStream(location));
	}

	public static byte[] loadBinary(InputStream stream) throws IOException {
		try {
			byte[] bin = new byte[stream.available()];
			StreamUtil.effectiveRead(stream, bin, 0, bin.length);
			stream.close();
			return bin;
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static ByteBuffer loadBinaryToBuffer(String location, ByteBuffer buffer) throws IOException {
		return loadBinaryToBuffer(getStream(location), buffer);
	}

	public static ByteBuffer loadBinaryToBuffer(InputStream stream, ByteBuffer buffer) throws IOException {
		try {
			int available = stream.available();
			if (available > buffer.limit() - buffer.position()) {
				throw new IllegalArgumentException("Not enough space in buffer");
			}
			if (buffer.hasArray()) {
				StreamUtil.effectiveRead(stream, buffer.array(), buffer.position(), available);
				buffer.position(buffer.position() + available);
			} else {
				byte[] raw = new byte[available];
				StreamUtil.effectiveRead(stream, raw, 0, available);
				buffer.put(raw);
			}
			return buffer;
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static String loadText(String location) throws IOException {
		return loadText(getStream(location));
	}

	public static String loadText(InputStream stream) throws IOException {
		return new String(loadBinary(stream));
	}

	public static BufferedImage loadImage(String location) throws IOException {
		return loadImage(getStream(location));
	}

	public static BufferedImage loadImage(InputStream stream) throws IOException {
		try {
			return ImageIO.read(stream);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static InputStream getStream(String location) {
		return ResourceLoader.class.getResourceAsStream(location);
	}

	public static int getFileSize(String location) throws IOException {
		return getFileSize(getStream(location));
	}

	public static int getFileSize(InputStream stream) throws IOException {
		try {
			return stream.available();
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

}
