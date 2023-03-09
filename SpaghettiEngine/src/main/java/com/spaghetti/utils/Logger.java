package com.spaghetti.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.spaghetti.core.Game;

public class Logger {

	public static final Object loggerLock = new Object();

	protected SimpleDateFormat printFormatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");
	protected SimpleDateFormat logFormatter = new SimpleDateFormat("dd_MM_yyyy_HH_mm");

	protected static final String[] CODES = {"DEBUG", "INFO", "LOADING", "WARNING", "ERROR", "FATAL"};
	public static final int DEBUG_SEVERITY = 0, MIN_SEVERITY = DEBUG_SEVERITY;
	public static final int INFO_SEVERITY = 1;
	public static final int LOADING_SEVERITY = 2;
	public static final int WARNING_SEVERITY = 3;
	public static final int ERROR_SEVERITY = 4;
	public static final int FATAL_SEVERITY = 5, MAX_SEVERITY = FATAL_SEVERITY;

	protected Game game;
	protected Logger superLogger;
	protected String superPrefix;
	protected int printSeverity = -2;
	protected int logSeverity = -2;
	protected PrintStream logDevice;

	public Logger(Game game) {
		this.game = game;
	}

	protected Logger(Logger superLogger, String superPrefix) {
		this.superLogger = superLogger;
		this.superPrefix = "[" + superPrefix + "] ";
	}

	public Logger getSubLogger(String prefix) {
		return new Logger(this, prefix);
	}

	// Utility

	protected String getPrintDate() {
		return "[" + printFormatter.format(new Date()) + "]";
	}

	protected String getLogDate() {
		return logFormatter.format(new Date());
	}

	protected void print(int severity, String string) {
		if(superLogger != null) {
			superLogger.print(severity, superPrefix + string);
			return;
		}
		if(printSeverity < -1 || logSeverity < -1) {
			printSeverity = game.<Integer>getEngineSetting("logseverityprint");
			logSeverity = game.<Integer>getEngineSetting("logseveritylog");

			if(printSeverity < -1 || logSeverity < -1) {
				printSeverity = -1;
				logSeverity = -1;
			}
		}

		String line = getPrintDate() + "[GAME " + game.getIndex() + "][" + thread() + "][" +
				CODES[(int) MathUtil.clamp(severity, MIN_SEVERITY, MAX_SEVERITY)] + "]: " + string;

		// Print to console
		if(severity >= printSeverity) {
			(severity >= ERROR_SEVERITY ? System.err : System.out).println(line);
		}

		// Print to file
		if(logDevice == null) {
			if(game.<Boolean>getEngineSetting("logautocreate")) {
				File folder = new File("./logs");
				if(!folder.exists()) {
					folder.mkdir();
				}

				final String prefix = "./logs/";
				final String suffix = ".log";
				int number = 1;
				File logfile = new File(prefix + getLogDate() + suffix);

				// Find a suitable name
				while(logfile.exists()) {
					logfile = new File(prefix + getLogDate() + "_" + number + suffix);
					number++;
				}

				// Set the file as the output device
				try {
					logfile.createNewFile();
					setLogDevice(new PrintStream(new FileOutputStream(logfile)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		if(severity >= logSeverity) {
			logDevice.println(line);
		}
	}

	protected static String exception(Throwable throwable) {
		String res = throwable.getClass().getName() + ": " + throwable.getMessage() + "\n";
		StackTraceElement[] s = throwable.getStackTrace();
		for (int i = 0; i < s.length; i++) {
			res += s[i].toString() + (i == s.length - 1 ? "" : "\n");
		}
		if (throwable.getCause() != null) {
			res += "\nCaused by ";
			res += exception(throwable.getCause());
		}
		return res;
	}

	protected String thread() {
		return Thread.currentThread().getName();
	}

	protected static Logger logger() {
		return Game.getInstance().getLogger();
	}

	protected static Logger logger(Game game) {
		return game.getLogger();
	}

	// Configuration

	public void setPrintSeverity(int severity) {
		printSeverity = severity;
	}

	public int getPrintSeverity() {
		return printSeverity;
	}

	public void setLogSeverity(int severity) {
		logSeverity = severity;
	}

	public int getLogSeverity() {
		return logSeverity;
	}

	public void setLogDevice(PrintStream stream) {
		synchronized(loggerLock) {
			logDevice = stream;
		}
	}

	public OutputStream getLogDevice() {
		synchronized(loggerLock) {
			return logDevice;
		}
	}

	// Instance methods

	public void printDebug(String string) {
		synchronized(loggerLock) {
			print(DEBUG_SEVERITY, string);
		}
	}

	public void printDebug(String message, Throwable t) {
		synchronized(loggerLock) {
			print(DEBUG_SEVERITY, message);
			print(DEBUG_SEVERITY, exception(t));
		}
	}

	public void printInfo(String string) {
		synchronized(loggerLock) {
			print(INFO_SEVERITY, string);
		}
	}

	public void printInfo(String message, Throwable t) {
		synchronized(loggerLock) {
			print(INFO_SEVERITY, message);
			print(INFO_SEVERITY, exception(t));
		}
	}

	public void printLoading(String string) {
		synchronized(loggerLock) {
			print(LOADING_SEVERITY, string);
		}
	}

	public void printLoading(String message, Throwable t) {
		synchronized(loggerLock) {
			print(LOADING_SEVERITY, message);
			print(LOADING_SEVERITY, exception(t));
		}
	}

	public void printWarning(String string) {
		synchronized(loggerLock) {
			print(WARNING_SEVERITY, string);
		}
	}

	public void printWarning(String message, Throwable t) {
		synchronized(loggerLock) {
			print(WARNING_SEVERITY, message);
			print(WARNING_SEVERITY, exception(t));
		}
	}

	public void printError(String string) {
		synchronized(loggerLock) {
			print(ERROR_SEVERITY, string);
		}
	}

	public void printError(String message, Throwable t) {
		synchronized(loggerLock) {
			print(ERROR_SEVERITY, message);
			print(ERROR_SEVERITY, exception(t));
		}
	}

	public void printFatal(String string) {
		synchronized(loggerLock) {
			print(FATAL_SEVERITY, string);
		}
	}

	public void printFatal(String message, Throwable t) {
		synchronized(loggerLock) {
			print(FATAL_SEVERITY, message);
			print(FATAL_SEVERITY, exception(t));
		}
	}

	// Static methods

	public static void debug(String string) {
		logger().printDebug(string);
	}

	public static void debug(String message, Throwable t) {
		logger().printDebug(message, t);
	}

	public static void info(String string) {
		logger().printInfo(string);
	}

	public static void info(String message, Throwable t) {
		logger().printInfo(message, t);
	}

	public static void loading(String string) {
		logger().printLoading(string);
	}

	public static void loading(String message, Throwable t) {
		logger().printLoading(message, t);
	}

	public static void warning(String string) {
		logger().printWarning(string);
	}

	public static void warning(String message, Throwable t) {
		logger().printWarning(message, t);
	}

	public static void error(String string) {
		logger().printError(string);
	}

	public static void error(String message, Throwable t) {
		logger().printError(message, t);
	}

	public static void fatal(String string) {
		logger().printFatal(string);
	}

	public static void fatal(String message, Throwable t) {
		logger().printFatal(message, t);
	}

	// Determine game instance directly

	public static void debug(Game game, String string) {
		logger(game).printDebug(string);
	}

	public static void debug(Game game, String message, Throwable t) {
		logger(game).printDebug(message, t);
	}

	public static void info(Game game, String string) {
		logger(game).printInfo(string);
	}

	public static void info(Game game, String message, Throwable t) {
		logger(game).printInfo(message, t);
	}

	public static void loading(Game game, String string) {
		logger(game).printLoading(string);
	}

	public static void loading(Game game, String message, Throwable t) {
		logger(game).printLoading(message, t);
	}

	public static void warning(Game game, String string) {
		logger(game).printWarning(string);
	}

	public static void warning(Game game, String message, Throwable t) {
		logger(game).printWarning(message, t);
	}

	public static void error(Game game, String string) {
		logger(game).printError(string);
	}

	public static void error(Game game, String message, Throwable t) {
		logger(game).printError(message, t);
	}

	public static void fatal(Game game, String string) {
		logger(game).printFatal(string);
	}

	public static void fatal(Game game, String message, Throwable t) {
		logger(game).printFatal(message, t);
	}

}
