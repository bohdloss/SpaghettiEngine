package com.spaghetti.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;

public final class Utils {

	private Utils() {
	}

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void sleep(long ms, int nanos) {
		try {
			Thread.sleep(ms, nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static final ByteBuffer parseImage(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();

		// Prepare for copy
		int pixels_raw[] = new int[w * h * 4];
		pixels_raw = img.getRGB(0, 0, w, h, null, 0, w);
		ByteBuffer pixels = BufferUtils.createByteBuffer(w * h * 4);

		// Copy data into a byte w_buffer
		for (int pixel : pixels_raw) {
			pixels.put((byte) ((pixel >> 16) & 0xFF)); // Red
			pixels.put((byte) ((pixel >> 8) & 0xFF)); // Green
			pixels.put((byte) ((pixel) & 0xFF)); // Blue
			pixels.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
		}

		pixels.flip();

		return pixels;
	}

	public static final void effectiveRead(InputStream stream, byte[] buffer, int offset, int amount) throws IOException {
		int read = offset, status = 0;
		while (read < amount || read == -1) {
			status = stream.read(buffer, read, amount - read);
			read += status;
		}
	}
	
}
