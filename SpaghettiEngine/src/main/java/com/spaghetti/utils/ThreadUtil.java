package com.spaghetti.utils;

import com.spaghetti.core.Game;
import com.spaghetti.core.GameThread;
import com.spaghetti.dispatcher.FunctionDispatcher;

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
	 * is and {@link GameThread} instance
	 */
	public static void yield() {
		Thread thread = Thread.currentThread();
		Game game = Game.getInstance();
		for(int i = 0; i < game.getThreadAmount(); i++) {
			GameThread gameThread = game.getThreadAt(i);
			if(gameThread.getThread().getId() == thread.getId()) {
				gameThread.getDispatcher().computeEvents();
				break;
			}
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
