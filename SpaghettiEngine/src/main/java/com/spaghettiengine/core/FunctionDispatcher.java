package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class FunctionDispatcher {

	// This class allows other threads to assign some tasks to the main thread
	// This can be useful for example when another thread needs to perform OpenGL
	// calls

	private HashMap<Long, Function<Object>> calls = new HashMap<>();
	private HashMap<Long, Object> returnValues = new HashMap<>();
	private List<Long> hasReturn = new ArrayList<>();
	private HashMap<Long, Throwable> exceptions = new HashMap<>();
	private List<Long> hasException = new ArrayList<>();
	private List<Long> ignoreReturn = new ArrayList<>();

	private final long executingThread;

	public FunctionDispatcher(long executingThread) {
		this.executingThread = executingThread;
	}

	public synchronized long queue(Function<Object> function) {
		return queue(function, false);
	}

	public synchronized long queue(Function<Object> function, boolean ignoreReturnValue) {
		if (ignoreReturnValue) {
			ignoreReturn.add(function.getId());
		}

		long thread = Thread.currentThread().getId();
		if (thread == executingThread) {
			processFunction(function.getId(), function);
		} else {
			calls.put(function.getId(), function);
		}
		return function.getId();
	}

	public Object waitReturnValue(long funcId) throws Throwable {
		while (!hasReturnValue(funcId) && !hasException(funcId)) {
			try {
				Thread.sleep(0);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return getReturnValue(funcId);
	}

	public synchronized boolean hasReturnValue(long funcId) {
		return hasReturn.contains(funcId);
	}

	public synchronized boolean hasException(long funcId) {
		return hasException.contains(funcId);
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

	public synchronized void computeEvents() {
		calls.forEach((id, function) -> {
			processFunction(id, function);
		});
		calls.clear();
	}

	private synchronized void processFunction(long id, Function<Object> function) {
		try {
			Object ret = function.execute();
			if (!ignoreReturn.contains(id)) {
				returnValues.put(id, ret);
				hasReturn.add(id);
			} else {
				ignoreReturn.remove(id);
			}
		} catch (Throwable e) {
			hasException.add(id);
			exceptions.put(id, e);
		}
	}

}
