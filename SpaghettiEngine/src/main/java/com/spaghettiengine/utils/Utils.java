package com.spaghettiengine.utils;

public final class Utils {

	private Utils() {}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
