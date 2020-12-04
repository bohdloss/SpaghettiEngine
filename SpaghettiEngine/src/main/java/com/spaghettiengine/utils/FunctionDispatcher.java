package com.spaghettiengine.utils;

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
	
	public synchronized long queue(Function function) {
		return queue(function, defaultId);
	}
	
	public synchronized long queue(Function function, long thread) {
		return queue(function, thread, false);
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
		while (!hasReturnValue(funcId) && !hasException(funcId)) {
			Utils.sleep(1);
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
		removeCache.clear();
		long thread = Thread.currentThread().getId();
		
		calls.forEach((id, function) -> {
			if(function.thread == thread) {
				processFunction(id, function);
				removeCache.add(id);
			}
		});
		
		removeCache.forEach(id -> {
			calls.remove(id);
		});
	}

	private synchronized void processFunction(long id, Function function) {
		try {
			Object ret = function.execute();
			if (!ignoreReturn.contains(id)) {
				returnValues.put(id, ret);
				hasReturn.add(id);
			}
		} catch (Throwable e) {
			hasException.add(id);
			exceptions.put(id, e);
		} finally {
			ignoreReturn.remove(id);
		}
	}

}
