package com.spaghettiengine.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.spaghettiengine.core.Game;

public final class Logger {

	private static final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

	private static final String INFO = "INFO", LOADING = "LOADING", ERROR = "ERROR", WARNING = "WARNING";

	private Logger() {
	}

	// Utility

	private static String date() {
		Date date = new Date();
		return "[" + formatter.format(date) + "]";
	}

	private static void print(String prefix, String string) {
		Game game = Game.getGame();
		if (game == null) {
			return;
		}

		print(game, prefix, string);
	}

	private static void print(Game game, String prefix, String string) {
		(prefix == ERROR ? System.err : System.out)
				.println(date() + "[GAME " + game.getIndex() + "][" + thread() + "][" + prefix + "]: " + string);
	}

	public static String exception(Throwable throwable) {
		String res = throwable.getClass().getName() + ": " + throwable.getMessage() + "\n";
		StackTraceElement[] s = throwable.getStackTrace();
		for (int i = 0; i < s.length; i++) {
			res += "" + s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		return res;
	}

	private static String thread() {
		return Thread.currentThread().getName();
	}

	// Determine game instance using current thread

	public static void info(String string) {
		print(INFO, string);
	}

	public static void loading(String string) {
		print(LOADING, string);
	}

	public static void error(String string) {
		print(ERROR, string);
	}

	public static void warning(String string) {
		print(WARNING, string);
	}

	public static void error(String message, Throwable t) {
		print(ERROR, message);
		print(ERROR, exception(t));
	}

	// Determine game instance directly

	public static void info(Game game, String string) {
		print(game, INFO, string);
	}

	public static void loading(Game game, String string) {
		print(game, LOADING, string);
	}

	public static void error(Game game, String string) {
		print(game, ERROR, string);
	}

	public static void warning(Game game, String string) {
		print(game, WARNING, string);
	}

	public static void error(Game game, String message, Throwable t) {
		print(game, ERROR, message);
		print(game, ERROR, exception(t));
	}

}
