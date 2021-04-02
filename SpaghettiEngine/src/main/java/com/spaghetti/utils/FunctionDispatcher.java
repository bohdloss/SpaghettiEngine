package com.spaghetti.utils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;

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
	private ThreadLocalRandom random;

	private Thread thread;

	// Queue using reflection

	public FunctionDispatcher() {
		this.thread = Thread.currentThread();
		this.random = ThreadLocalRandom.current();
	}

	public synchronized long queue(boolean ignoreReturnValue, Object target, String funcName, Object... args) {

		try {

			// Gather class types of the arguments

			Class<?> cls = target.getClass();
			Class<?>[] classes = new Class<?>[args.length];

			for (int i = 0; i < args.length; i++) {
				classes[i] = args[i].getClass();
			}

			// Find the method

			Method method = cls.getMethod(funcName, classes);
			method.setAccessible(true);

			// Queue
			Function toQueue = () -> method.invoke(target, args);

			return queue(toQueue, ignoreReturnValue);

		} catch (Throwable t) {
			throw new IllegalArgumentException();
		}

	}

	public synchronized long queue(Object target, String funcName, Object... args) {
		return queue(false, target, funcName, args);
	}

	// Queue by function object

	public synchronized long queue(Function function) {
		return queue(function, false);
	}

	public synchronized long queue(Function function, boolean ignoreReturnValue) {
		if (function == null) {
			throw new IllegalArgumentException();
		}

		long rand = random.nextLong();

		if (ignoreReturnValue) {
			ignoreReturn.add(rand);
		}

		if (thread.getId() == Thread.currentThread().getId()) {
			processFunction(rand, function);
		} else {
			calls.put(rand, function);
		}
		return rand;
	}

	// Quick queue

	public Object quickQueue(Function function) {
		long func = queue(function);
		Object ret;
		try {
			ret = waitReturnValue(func);
		} catch (Throwable e) {
			ret = e;
		}
		return ret;
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
		while (calls.containsKey(funcId)) {
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

	public synchronized void computeEvents(int amount) {
		if (Thread.currentThread().getId() != thread.getId()) {
			return;
		}

		int i = 0;
		for (Entry<Long, Function> entry : calls.entrySet()) {
			if (i >= amount) {
				return;
			}
			processFunction(entry.getKey(), entry.getValue());
			i++;
		}
		calls.clear();
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

	public synchronized int getAmount() {
		return calls.size();
	}

}
