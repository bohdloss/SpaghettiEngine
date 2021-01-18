package com.spaghettiengine.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FunctionDispatcher {

	// This class allows other threads to assign some tasks to the main thread
	// This can be useful for example when another thread needs to perform OpenGL
	// calls

	private HashMap<Long, Function> calls = new HashMap<>();
	private HashMap<Long, Object> returnValues = new HashMap<>();
	private List<Long> hasReturn = new ArrayList<>();
	private HashMap<Long, Throwable> exceptions = new HashMap<>();
	private List<Long> hasException = new ArrayList<>();
	private List<Long> ignoreReturn = new ArrayList<>();

	private ArrayList<Long> removeCache = new ArrayList<>();

	private long defaultId;

	public synchronized void setDefaultId(long id) {
		defaultId = id;
	}

	public synchronized long getDefaultId() {
		return defaultId;
	}

	// Queue using reflection

	public synchronized long queue(long thread, boolean ignoreReturnValue, Object target, String funcName,
			Object... args) {

		try {

			// Gather class types of the arguments

			Class<?> cls = target.getClass();
			Class<?>[] classes = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				classes[i] = args[i].getClass();
			}

			// Find the method

			Method method = cls.getMethod(funcName, classes);

			Function toQueue = new Function(() -> method.invoke(target, args));

			return queue(toQueue, thread, ignoreReturnValue);

		} catch (Throwable t) {
			throw new IllegalArgumentException();
		}

	}

	public synchronized long queue(long thread, Object target, String funcName, Object... args) {
		return queue(thread, false, target, funcName, args);
	}

	public synchronized long queue(boolean ignoreReturnValue, Object target, String funcName, Object... args) {
		return queue(defaultId, ignoreReturnValue, target, funcName, args);
	}

	public synchronized long queue(Object target, String funcName, Object... args) {
		return queue(defaultId, false, target, funcName, args);
	}

	// Queue by function object

	public synchronized long queue(Function function) {
		return queue(function, defaultId);
	}

	public synchronized long queue(Function function, long thread) {
		return queue(function, thread, false);
	}

	public synchronized long queue(Function function, boolean ignoreReturnValue) {
		return queue(function, defaultId, ignoreReturnValue);
	}

	public synchronized long queue(Function function, long thread, boolean ignoreReturnValue) {
		function.thread = thread;

		if (ignoreReturnValue) {
			ignoreReturn.add(function.getId());
		}

		long cur = Thread.currentThread().getId();
		if (cur == function.thread) {
			processFunction(function.getId(), function);
		} else {
			calls.put(function.getId(), function);
		}
		return function.getId();
	}

	public Object waitReturnValue(long funcId) throws Throwable {
		if (ignoresReturnValue(funcId)) {
			return null;
		}
		while (!hasReturnValue(funcId) && !hasException(funcId)) {
			Utils.sleep(1);
		}
		return getReturnValue(funcId);
	}

	public void waitFor(long funcId) {
		while (calls.get(funcId) != null) {
			Utils.sleep(1);
		}
	}

	public synchronized boolean hasReturnValue(long funcId) {
		return hasReturn.contains(funcId);
	}

	public synchronized boolean hasException(long funcId) {
		return hasException.contains(funcId);
	}

	public synchronized boolean ignoresReturnValue(long funcId) {
		return ignoreReturn.contains(funcId);
	}

	public synchronized Object getReturnValue(long funcId) throws Throwable {
		if (hasException(funcId)) {
			hasException.remove(funcId);
			throw exceptions.remove(funcId);
		} else if (hasReturnValue(funcId)) {
			hasReturn.remove(funcId);
			return returnValues.remove(funcId);
		} else {
			return null;
		}
	}

	private int current;

	public synchronized void computeEvents(int amount) {
		current = 0;
		removeCache.clear();
		long thread = Thread.currentThread().getId();

		calls.forEach((id, function) -> {
			if (function.thread == thread && current < amount) {
				processFunction(id, function);
				removeCache.add(id);
				current++;
			}
		});

		removeCache.forEach(id -> {
			calls.remove(id);
		});
	}

	public synchronized void computeEvents() {
		computeEvents(Integer.MAX_VALUE);
	}

	private synchronized void processFunction(long id, Function function) {
		try {
			Object ret = function.execute();
			if (!ignoreReturn.contains(id)) {
				returnValues.put(id, ret);
				hasReturn.add(id);
			}
		} catch (Throwable e) {
			if (!ignoreReturn.contains(id)) {
				hasException.add(id);
				exceptions.put(id, e);
			}
		} finally {
			ignoreReturn.remove(id);
		}
	}

}
