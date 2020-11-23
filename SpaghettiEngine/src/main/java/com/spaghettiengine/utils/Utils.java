package com.spaghettiengine.utils;

public class Utils {

	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
	}
	
}
