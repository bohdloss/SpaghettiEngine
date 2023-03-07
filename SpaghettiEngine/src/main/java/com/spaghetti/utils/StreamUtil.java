package com.spaghetti.utils;

import com.spaghetti.core.CoreComponent;
import org.lwjgl.assimp.AIString;
import org.lwjgl.assimp.Assimp;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC10;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL45;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * ThreadUtil is a namespace for common useful stream functions
 *
 * @author bohdloss
 *
 */
public final class StreamUtil {

	private StreamUtil() {
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
			byte[] arr = new byte[amount];
			int read = effectiveRead(stream, arr, 0, amount);
			buffer.put(arr, offset, amount);
			return read;
		}
	}

	/**
	 * Closes a Closeable interface and catches any exception
	 *
	 * @param closeable The closeable interface
	 * @return {@code false} if an exception was thrown, {@code true} otherwise
	 */
	public static Throwable close(Closeable closeable) {
		try {
			closeable.close();
			return null;
		} catch (Throwable t) {
			return t;
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

}
