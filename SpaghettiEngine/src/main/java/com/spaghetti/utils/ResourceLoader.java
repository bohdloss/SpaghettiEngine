package com.spaghetti.utils;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;

public final class ResourceLoader {

	/*
	 * Utility class that abstracts the loading of various file types into objects
	 * such as images, binary and text files
	 */

	private ResourceLoader() {
	}

	public static final byte[] loadBinary(String location) throws Throwable {
		InputStream stream = null;
		try {
			stream = ResourceLoader.class.getResourceAsStream(location);
			byte[] bin = new byte[stream.available()];
			Utils.effectiveRead(stream, bin, 0, bin.length);
			stream.close();
			return bin;
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static final String loadText(String location) throws Throwable {
		return new String(loadBinary(location));
	}

	public static final BufferedImage loadImage(String location) throws Throwable {
		InputStream stream = null;
		try {
			stream = ResourceLoader.class.getResourceAsStream(location);
			return ImageIO.read(stream);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public static final InputStream getStream(String location) {
		return ResourceLoader.class.getResourceAsStream(location);
	}

}
