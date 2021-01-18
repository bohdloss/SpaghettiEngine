package com.spaghettiengine.utils;

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

}
