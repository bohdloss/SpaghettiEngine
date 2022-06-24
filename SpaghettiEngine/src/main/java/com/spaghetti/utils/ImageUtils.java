package com.spaghetti.utils;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import org.lwjgl.system.MemoryUtil;

/**
 * ImageUtils is a namespace for functions related to image manipulation through
 * ByteBuffer's
 * 
 * @author bohdloss
 *
 */
public final class ImageUtils {

	private ImageUtils() {
	}

	/**
	 * Allocates a new ByteBuffer on the heap for a RGBA formatted image, having a
	 * size of {@code width * height * 4} bytes <br>
	 * The returned ByteBuffer is not guaranteed to be zeroed
	 * 
	 * @param width  The width of the image
	 * @param height The height of the image
	 * @return The ByteBuffer representing the allocated image
	 */
	public static ByteBuffer allocateImage(int width, int height) {
		return MemoryUtil.memAlloc(width * height * 4);
	}

	/**
	 * Deallocates the specified ByteBuffer image from the heap <br>
	 * Freeing a ByteBuffer allocated in any other method other than with
	 * {@link #allocateImage(int, int)} will result in undefined behavior
	 * 
	 * @param image
	 */
	public static void freeImage(ByteBuffer image) {
		MemoryUtil.memFree(image);
	}

	/**
	 * Converts a BufferedImage into an OpenGL-compatible ByteBuffer <br>
	 * If passed {@code null} as the output parameter a new buffer will be allocated
	 * with {@link #allocateImage(int, int)} using the size of the image as
	 * arguments
	 * 
	 * @param image  The image to convert
	 * @param output The ByteBuffer to store the result in
	 * @return The converted byte buffer
	 */
	public static ByteBuffer parseImage(BufferedImage image, ByteBuffer output) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (output == null) {
			output = allocateImage(width, height);
		} else if (output.capacity() < (width * height * 4)) {
			throw new IllegalArgumentException("The provided buffer is not big enough to contain the converted image");
		}

		// Prepare for copy
		int pixels_raw[] = new int[width * height];
		image.getRGB(0, 0, width, height, pixels_raw, 0, width);

		// Copy data into a byte buffer
		for (int pixel : pixels_raw) {
			output.put((byte) ((pixel >> 16) & 0xFF)); // Red
			output.put((byte) ((pixel >> 8) & 0xFF)); // Green
			output.put((byte) ((pixel) & 0xFF)); // Blue
			output.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
		}
		output.flip();

		return output;
	}

}
