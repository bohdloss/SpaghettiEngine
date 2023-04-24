package com.spaghetti.utils;

import com.spaghetti.exceptions.GLException;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;

/**
 * ThreadUtil is a namespace for common useful thread functions
 *
 * @author bohdloss
 *
 */
public final class ExceptionUtil {

	private ExceptionUtil() {
	}

	// TODO Temporary, remove later
	static int num;

	/**
	 * Retrieves the last OpenAL error and prints the stack trace in the console
	 * <p>
	 * The usage of this function is recommended after every OpenAL call for good
	 * practice
	 */
	public static void alError() {
		if (num >= 10) {
			System.exit(0);
		}
		int error = AL10.alGetError();
		if (error == AL10.AL_NO_ERROR) {
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

	public static void glConsumeError() {
		GL11.glGetError();
	}

	/**
	 * Retrieves the last OpenALC error in the {@code deviceHandle} device and
	 * prints the stack trace in the console
	 * <p>
	 * The usage of this function is recommended after every OpenALC call for good
	 * practice
	 *
	 * @param deviceHandle The handle to the OpenALC device from which to retrieve
	 *                     the error code
	 */
	public static void alcError(long deviceHandle) {
		if (num >= 10) {
			System.exit(0);
		}
		int error = ALC10.alcGetError(deviceHandle);
		if (error == ALC10.ALC_NO_ERROR) {
			return;
		}
		String error_str = "OpenALC error detected: ";
		switch (error) {
			case ALC10.ALC_INVALID_CONTEXT:
				error_str += "ALC_INVALID_CONTEXT";
				break;
			case ALC10.ALC_INVALID_DEVICE:
				error_str += "ALC_INVALID_DEVICE";
				break;
			case ALC10.ALC_INVALID_ENUM:
				error_str += "ALC_INVALID_ENUM";
				break;
			case ALC10.ALC_INVALID_VALUE:
				error_str += "ALC_INVALID_VALUE";
				break;
			case ALC10.ALC_OUT_OF_MEMORY:
				error_str += "ALC_OUT_OF_MEMORY";
				break;
			default:
				error_str += "Unknown error";
				break;
		}
		error_str += "\n";
		if (deviceHandle == 0) {
			error_str += "WARNING: No valid device handle specified\n";
		}
		StackTraceElement[] s = Thread.currentThread().getStackTrace();
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
		num++;
	}

	/**
	 * Retrieves the last OpenGL error and prints the stack trace in the console
	 * <p>
	 * The usage of this function is recommended after every OpenGL call for good
	 * practice
	 */
	public static void glError() {
		int error = GL11.glGetError();
		if (error == GL11.GL_NO_ERROR) {
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
		StackTraceElement[] ns = new StackTraceElement[(int) MathUtil.clampMin(s.length - 2, 0)];
		for (int i = 2; i < s.length; i++) {
			//error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
			ns[i - 2] = s[i];
		}
		throw new GLException(ns);
	}

	/**
	 * Retrieves the last Assimp error and prints the stack trace in the console
	 * <p>
	 * The usage of this function is recommended after every major Assimp call for
	 * good practice
	 */
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

	/**
	 * Converts an AIString to a standard java String
	 *
	 * @param string The Assimp string to convert
	 * @return The converted string
	 */
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

	/**
	 * Prints the current thread stack trace in a similar way as
	 * {@link Throwable#printStackTrace()} does
	 */
	public static void printStackTrace() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StringBuilder trace = new StringBuilder();
		for (int i = 2; i < stack.length; i++) {
			trace.append(stack[i].getClassName());
			trace.append('.');
			trace.append(stack[i].getMethodName());
			trace.append('(');
			trace.append(stack[i].getFileName());
			trace.append(':');
			trace.append(stack[i].getLineNumber());
			trace.append(')');
			if (i != stack.length - 1) {
				trace.append('\n');
			}
		}
		Logger.info(trace.toString());
	}

}
