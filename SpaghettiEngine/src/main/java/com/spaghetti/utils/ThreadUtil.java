package com.spaghetti.utils;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

import com.spaghetti.core.CoreComponent;

/**
 * ThreadUtil is a namespace for common useful thread functions
 *
 * @author bohdloss
 *
 */
public final class ThreadUtil {

	private ThreadUtil() {
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
			Logger.error("", e);
		}
	}

	/**
	 * Waits until the current time is equal or greater than {@code time}
	 *
	 * @param time The time until which to wait
	 */
	public static void sleepUntil(long time) {
		while (System.currentTimeMillis() < time) {
			sleep(1);
		}
	}

	/**
	 * Yields execution to the current thread's
	 * {@link FunctionDispatcher#computeEvents()} method only if the current thread
	 * is and {@link CoreComponent} instance
	 */
	public static void yield() {
		Thread thread = Thread.currentThread();
		if (CoreComponent.class.isAssignableFrom(thread.getClass())) {
			CoreComponent core = (CoreComponent) thread;
			core.getDispatcher().computeEvents();
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
			Logger.error("", e);
		}
	}

}
