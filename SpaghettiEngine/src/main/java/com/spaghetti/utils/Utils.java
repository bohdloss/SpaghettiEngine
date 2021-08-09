package com.spaghetti.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

/**
 * Utils is a namespace for common useful functions
 * 
 * @author bohdloss
 *
 */
public final class Utils {

	private Utils() {
	}

	/**
	 * Sleeps for the given amount of {@code ms} a and catches the
	 * InterruptedException
	 * 
	 * @param ms Amount of milliseconds to sleep for
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Waits until the current time is equal or greater than {@code time}
	 * 
	 * @param time The time until which to wait
	 */
	public static void sleepUntil(long time) {
		while (System.currentTimeMillis() < time) {
			sleep(0);
		}
	}

	/**
	 * Sleeps for the given amount of {@code ms} and {@code nanos} and catches the
	 * InterruptedException
	 * 
	 * @param ms    Amount of milliseconds to sleep for
	 * @param nanos Amount of nanosecond to sleep for in addition to the
	 *              milliseconds
	 */
	public static void sleep(long ms, int nanos) {
		try {
			Thread.sleep(ms, nanos);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Keeps reading from the given {@code stream}, until {@code amount} bytes have
	 * been read, the end of the stream is reached or an exception is thrown by the
	 * {@code stream}, starting at the given {@code offset} in the {@code buffer}
	 * 
	 * @param stream The stream to read data from
	 * @param buffer The buffer in which to read the data
	 * @param offset The offset in the buffer at which to start writing data
	 * @param amount The amount of bytes to be read from the stream
	 * @return The number of bytes actually read, in case the end of the stream is
	 *         reached
	 * @throws IOException
	 */
	public static int effectiveRead(InputStream stream, byte[] buffer, int offset, int amount) throws IOException {
		int read = offset, status = 0;
		while (read < amount && status != -1) {
			status = stream.read(buffer, read, amount - read);
			read += status;
		}
		if (status == -1) {
			read += 1;
		}
		return read;
	}

	/**
	 * Keeps reading from the given {@code stream}, until {@code amount} bytes have
	 * been read, the end of the stream is reached, an exception is thrown by the
	 * {@code stream} or {@code timeout} milliseconds have passed since the method
	 * invocation, starting at the given {@code offset} in the {@code buffer}
	 * 
	 * @param stream  The stream to read data from
	 * @param buffer  The buffer in which to read the data
	 * @param offset  The offset in the buffer at which to start writing data
	 * @param amount  The amount of bytes to be read from the stream
	 * @param timeout The timeout in milliseconds after which the operation will be
	 *                interrupted
	 * @return The number of bytes actually read, in case the end of the stream is
	 *         reached
	 * @throws IOException
	 */
	public static int effectiveReadTimeout(InputStream stream, byte[] buffer, int offset, int amount, long timeout)
			throws IOException {
		long time = System.currentTimeMillis();
		int read = offset, status = 0;
		while (read < amount && status != -1) {
			status = stream.read(buffer, read, amount - read);
			read += status;
			if (System.currentTimeMillis() > time + timeout && timeout != 0) {
				throw new IllegalStateException("Read timeout of " + timeout + " ms reached");
			}
		}
		if (status == -1) {
			read += 1;
		}
		return read;
	}

	/**
	 * Keeps reading from the given {@code stream}, until {@code amount} bytes have
	 * been read, the end of the stream is reached or an exception is thrown by the
	 * {@code stream}, starting at {@code offset + buffer.position()} in the
	 * {@code buffer}
	 * 
	 * @param stream The stream to read data from
	 * @param buffer The buffer in which to read the data
	 * @param offset The offset that will be added to the buffer's position at which
	 *               to start writing data
	 * @param amount The amount of bytes to be read from the stream
	 * @return The number of bytes actually read, in case the end of the stream is
	 *         reached
	 * @throws IOException
	 */
	public static int effectiveRead(InputStream stream, ByteBuffer buffer, int offset, int amount) throws IOException {
		if (buffer.hasArray()) {
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
			if (status == -1) {
				read += 1;
			}
			buffer.put(arr, 0, amount);
			return read;
		}
	}

	/**
	 * Closes a Closeable interface and catches any exception
	 * 
	 * @param closeable The closeable interface
	 * @return {@code false} if an exception was thrown, {@code true} otherwise
	 */
	public static boolean close(Closeable closeable) {
		try {
			closeable.close();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Closes a Socket's input stream and catches any exception
	 * 
	 * @param socket The Socket whose input stream to close
	 * @return {@code false} if an exception was thrown, {@code true} otherwise
	 */
	public static boolean socketCloseInput(Socket socket) {
		try {
			socket.shutdownInput();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Closes a Socket's output stream and catches any exception
	 * 
	 * @param socket The Socket whose output stream to close
	 * @return {@code false} if an exception was thrown, {@code true} otherwise
	 */
	public static boolean socketCloseOutput(Socket socket) {
		try {
			socket.shutdownOutput();
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	/**
	 * Returns the boolean value of the bit at {@code pos} index in the {@code num}
	 * 
	 * @param num The number whose bit is to extract
	 * @param pos The index at which to extract the bit
	 * @return The extracted bit in boolean form
	 */
	public static boolean bitAt(int num, int pos) {
		return (num & (1 << pos)) != 0;
	}

	/**
	 * Changes the bit at index {@code pos} inside of {@code num} to the value of
	 * {@code newval}, and returns the resulting edited {@code num}
	 * 
	 * @param num    The number to edit
	 * @param pos    The index at which to change the bit
	 * @param newval The new value to change the bit to
	 * @return The edit version of {@code num}
	 */
	public static int bitAt(int num, int pos, boolean newval) {
		int mask = newval ? (1 << pos) : ~(1 << pos);
		return newval ? (num | mask) : (num & mask);
	}

	/**
	 * Hashes the given string into a {@code long} value
	 * 
	 * @param str The string to hash
	 * @return The hash
	 */
	public static long longHash(String str) {
		long hash = 1125899906842597L;

		for (int i = 0; i < str.length(); i++) {
			hash = 31 * hash + str.charAt(i);
		}
		return hash;
	}

	/**
	 * Hashes the given byte array into a {@code long} value, only accounting for
	 * the bytes starting from {@code offset} until {@code offset + size}
	 * 
	 * @param mem    The byte array to hash
	 * @param offset The offset at which to start calculating the hash
	 * @param size   The amount of bytes to hash after the {@code offset}
	 * @return The hash
	 */
	public static long longHash(byte[] mem, int offset, int size) {
		long hash = 1125899906842597L;

		for (int i = offset; i < offset + size; i++) {
			hash = 31 * hash + mem[i];
		}
		return hash;
	}

	/**
	 * Hashes the given string into an {@code int} value
	 * 
	 * @param str The string to hash
	 * @return The hash
	 */
	public static int intHash(String str) {
		int hash = 13464481;

		for (int i = 0; i < str.length(); i++) {
			hash = 31 * hash + str.charAt(i);
		}
		return hash;
	}

	/**
	 * Hashes the given byte array into an {@code int} value, only accounting for
	 * the bytes starting from {@code offset} until {@code offset + size}
	 * 
	 * @param mem    The byte array to hash
	 * @param offset The offset at which to start calculating the hash
	 * @param size   The amount of bytes to hash after the {@code offset}
	 * @return The hash
	 */
	public static int intHash(byte[] mem, int offset, int size) {
		int hash = 13464481;

		for (int i = offset; i < offset + size; i++) {
			hash = 31 * hash + mem[i];
		}
		return hash;
	}

	/**
	 * Hashes the given string into a {@code short} value
	 * 
	 * @param str The string to hash
	 * @return The hash
	 */
	public static short shortHash(String str) {
		short hash = 14951;

		for (int i = 0; i < str.length(); i++) {
			hash = (short) (hash * 31 + str.charAt(i));
		}
		return hash;
	}

	/**
	 * Hashes the given byte array into a {@code short} value, only accounting for
	 * the bytes starting from {@code offset} until {@code offset + size}
	 * 
	 * @param mem    The byte array to hash
	 * @param offset The offset at which to start calculating the hash
	 * @param size   The amount of bytes to hash after the {@code offset}
	 * @return The hash
	 */
	public static short shortHash(byte[] mem, int offset, int size) {
		short hash = 14951;

		for (int i = offset; i < offset + size; i++) {
			hash = (short) (hash * 31 + mem[i]);
		}
		return hash;
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
		for (int i = 2; i < s.length; i++) {
			error_str += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		Logger.error(error_str);
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
	 * Obtains a private field with the given {@code name} from the given
	 * {@code cls} and if not found, the superclass of {@code cls} will be searched,
	 * and so on iteratively.<br>
	 * Once the field is found, {@code private/protected} and {@code final}
	 * restrictions are removed from it, then it is returned
	 * <p>
	 * This method will throw a RuntimeException if the field couldn't be obtained
	 * because of some exception, or if the field does not exist
	 * 
	 * @param cls  The class to start searching for the field
	 * @param name The name of the field to search for
	 * @return The Field
	 */
	public static Field getPrivateField(Class<?> cls, String name) {
		try {
			Field result = null;
			while (result == null) {
				try {
					// Remove private restrictions
					result = cls.getDeclaredField(name);
					result.setAccessible(true);

					// Remove final restrictions
					Field modifiers = Field.class.getDeclaredField("modifiers");
					modifiers.setAccessible(true);
					modifiers.set(result, result.getModifiers() & ~Modifier.FINAL);
				} catch (NoSuchFieldException nofield) {
					cls = cls.getSuperclass();
				}
			}
			return result;
		} catch (Throwable t) {
			throw new RuntimeException("Couldn't obtain field " + cls.getName() + "." + name, t);
		}
	}
	
	/**
	 * Obtains a private method with the given {@code name} and {@code arguments} vararg from the given
	 * {@code cls} and if not found, the superclass of {@code cls} will be searched,
	 * and so on iteratively.<br>
	 * Once the method is found, {@code private/protected}
	 * restrictions are removed from it, then it is returned
	 * <p>
	 * This method will throw a RuntimeException if the field couldn't be obtained
	 * because of some exception, or if the field does not exist
	 * 
	 * @param cls  The class to start searching for the method
	 * @param name The name of the method to search for
	 * @param arguments The argument types the method accepts
	 * @return The Method
	 */
	public static Method getPrivateMethod(Class<?> cls, String name, Class<?>...arguments) {
		try {
			Method result = null;
			while (result == null) {
				try {
					// Remove private restrictions
					result = cls.getDeclaredMethod(name, arguments);
					result.setAccessible(true);
				} catch (NoSuchMethodException nofield) {
					cls = cls.getSuperclass();
				}
			}
			return result;
		} catch (Throwable t) {
			throw new RuntimeException("Couldn't obtain method " + cls.getName() + "." + name, t);
		}
	}

	/**
	 * Prints the current thread stack trace in a similar way as {@link Throwable#printStackTrace()} does
	 */
	public static void printStackTrace() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		StringBuilder trace = new StringBuilder();
		for(int i = 2; i < stack.length; i++) {
			trace.append(stack[i].getClassName());
			trace.append('.');
			trace.append(stack[i].getMethodName());
			trace.append('(');
			trace.append(stack[i].getFileName());
			trace.append(':');
			trace.append(stack[i].getLineNumber());
			trace.append(')');
			if(i != stack.length - 1) {
				trace.append('\n');
			}
		}
		Logger.info(trace.toString());
	}
	
}
