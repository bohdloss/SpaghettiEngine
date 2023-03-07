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
 * ThreadUtil is a namespace for common useful thread functions
 *
 * @author bohdloss
 *
 */
public final class HashUtil {

	private HashUtil() {
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

}
