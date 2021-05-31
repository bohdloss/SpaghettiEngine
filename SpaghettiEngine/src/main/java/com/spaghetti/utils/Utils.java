package com.spaghetti.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC11;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

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

	public static void sleepUntil(long time) {
		while (System.currentTimeMillis() < time) {
			sleep(0);
		}
	}

	public static void sleep(long ms, int nanos) {
		try {
			Thread.sleep(ms, nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static ByteBuffer parseImage(BufferedImage img) {
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

	public static int effectiveRead(InputStream stream, byte[] buffer, int offset, int amount) throws IOException {
		int read = offset, status = 0;
		while (read < amount && status != -1) {
			status = stream.read(buffer, read, amount - read);
			read += status;
		}
		if(status == -1) {
			read += 1;
		}
		return read;
	}

	public static int effectiveRead(InputStream stream, ByteBuffer buffer, int offset, int amount) throws IOException {
		if(buffer.hasArray()) {
			int read = effectiveRead(stream, buffer.array(), buffer.position() + offset, amount);
			buffer.position(buffer.position() + amount);
			return read;
		} else {
			int read = offset, status = 0;
			byte[] arr = new byte[amount];
			while (read < amount && status != -1) {
				status = stream.read(arr, read, amount - read);
				read += status;
			}
			if(status == -1) {
				read += 1;
			}
			buffer.put(arr, 0, amount);
			return read;
		}
	}
	
	public static boolean socketClose(Socket socket) {
		try {
			socket.close();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean socketCloseInput(Socket socket) {
		try {
			socket.shutdownInput();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean socketCloseOutput(Socket socket) {
		try {
			socket.shutdownOutput();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static boolean bitAt(int num, int pos) {
		return (num & (1 << pos)) != 0;
	}

	public static int bitAt(int num, int pos, boolean newval) {
		int mask = newval ? (1 << pos) : ~(1 << pos);
		return newval ? (num | mask) : (num & mask);
	}

	public static long longHash(String str) {
		long hash = 1125899906842597L;

		for (int i = 0; i < str.length(); i++) {
			hash = 31 * hash + str.charAt(i);
		}
		return hash;
	}

	public static int intHash(String str) {
		int hash = 13464481;

		for (int i = 0; i < str.length(); i++) {
			hash = 31 * hash + str.charAt(i);
		}
		return hash;
	}

	public static short shortHash(String str) {
		short hash = 14951;

		for (int i = 0; i < str.length(); i++) {
			hash = (short) (hash * 31 + str.charAt(i));
		}
		return hash;
	}
static int num;
	public static void alError() {
		if(num >= 10) {
			System.exit(0);
		}
		int error = AL10.alGetError();
		if(error == AL11.AL_NO_ERROR) {
			return;
		}
		String error_str = "OpenAL error detected: ";
		switch (error) {
		case AL10.AL_INVALID_OPERATION:
			error_str += "AL_INVALID_OPERATION";
			break;
		case AL10.AL_INVALID_ENUM:
			error_str += "AL_INVALID_ENUM";
			break;
		case AL10.AL_INVALID_NAME:
			error_str += "AL_INVALID_NAME";
			break;
		case AL10.AL_INVALID_VALUE:
			error_str += "AL_INVALID_VALUE";
			break;
		case AL10.AL_OUT_OF_MEMORY:
			error_str += "AL_OUT_OF_MEMORY";
			break;
		default:
			error_str += "Unknown error";
			break;
		}
		error_str += "\n";
		try {
			AL.getCapabilities();
		} catch (IllegalStateException e) {
			error_str += "WARNING: No context is current\n";
		}
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
		num++;
	}

	public static void alcError(long deviceHandle) {
		if(num >= 10) {
			System.exit(0);
		}
		int error = ALC11.alcGetError(deviceHandle);
		if(error == ALC11.ALC_NO_ERROR) {
			return;
		}
		String error_str = "OpenALC error detected: ";
		switch (error) {
		case ALC11.ALC_INVALID_CONTEXT:
			error_str += "ALC_INVALID_CONTEXT";
			break;
		case ALC11.ALC_INVALID_DEVICE:
			error_str += "ALC_INVALID_DEVICE";
			break;
		case ALC11.ALC_INVALID_ENUM:
			error_str += "ALC_INVALID_ENUM";
			break;
		case ALC11.ALC_INVALID_VALUE:
			error_str += "ALC_INVALID_VALUE";
			break;
		case ALC11.ALC_OUT_OF_MEMORY:
			error_str += "ALC_OUT_OF_MEMORY";
			break;
		default:
			error_str += "Unknown error";
			break;
		}
		error_str += "\n";
		if(deviceHandle == 0) {
			error_str += "WARNING: No valid device handle specified\n";
		}
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
		num++;
	}
	
	public static void glError() {
		int error = GL11.glGetError();
		if(error == GL11.GL_NO_ERROR) {
			return;
		}
		String error_str = "OpenGL error detected: ";
		switch (error) {
		case GL11.GL_INVALID_ENUM:
			error_str += "GL_INVALID_ENUM";
			break;
		case GL30.GL_INVALID_FRAMEBUFFER_OPERATION:
			error_str += "GL_INVALID_FRAMEBUFFER_OPERATION";
			break;
		case GL11.GL_INVALID_OPERATION:
			error_str += "GL_INVALID_OPERATION";
			break;
		case GL11.GL_INVALID_VALUE:
			error_str += "GL_INVALID_VALUE";
			break;
		case GL11.GL_STACK_OVERFLOW:
			error_str += "GL_STACK_OVERFLOW";
			break;
		case GL11.GL_STACK_UNDERFLOW:
			error_str += "GL_STACK_UNDERFLOW";
			break;
		case GL11.GL_OUT_OF_MEMORY:
			error_str += "GL_OUT_OF_MEMORY";
			break;
		case GL45.GL_CONTEXT_LOST:
			error_str += "GL_CONTEXT_LOST";
			break;
		default:
			error_str += "Unknown error";
			break;
		}
		error_str += "\n";
		try {
			GL.getCapabilities();
		} catch (IllegalStateException e) {
			error_str += "WARNING: No context is current\n";
		}
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
	}

	public static void aiError() {
		String error = Assimp.aiGetErrorString();
		if (error == null || error.length() == 0) {
			return;
		}
		String error_str = "Assimp error detected: " + error + "\n";
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
	}

	public static String getAssimpString(AIString string) {
		ByteBuffer buf = string.data();
		if (buf.hasArray()) {
			return new String(buf.array());
		} else {
			byte[] arr = new byte[buf.capacity()];
			buf.get(arr);
			return new String(arr);
		}
	}

}
