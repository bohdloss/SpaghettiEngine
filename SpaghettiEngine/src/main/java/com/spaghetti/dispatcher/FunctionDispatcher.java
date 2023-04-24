package com.spaghetti.dispatcher;

import com.spaghetti.utils.MathUtil;
import com.spaghetti.utils.ThreadUtil;

import java.lang.reflect.Method;
import java.util.*;

public final class FunctionDispatcher {

	// This class allows other threads to assign some tasks to the main thread
	// This can be useful for example when another thread needs to perform OpenGL
	// calls
	private final List<FunctionWrapper> callQueue = new ArrayList<>();
	private final Map<Long, FunctionWrapper> callMap = new HashMap<>();

	private final Random random;
	private final Thread thread;

	// Queue using reflection

	public FunctionDispatcher(Thread thread) {
		this.thread = thread;
		this.random = new Random();
	}

	public long queue(boolean ignoreReturnValue, Object target, String funcName, Object... args) {

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
			throw new IllegalArgumentException(t);
		}

	}

	public long queue(Object target, String funcName, Object... args) {
		return queue(false, target, funcName, args);
	}

	// Queue by function object

	public long queue(Function function) {
		return queue(function, false);
	}

	public long queueVoid(VoidFunction function) {
		return queue(function, false);
	}

	public long queueVoid(VoidFunction function, boolean ignoreReturnValue) {
		return queue(function, ignoreReturnValue);
	}

	public long queue(Function function, boolean ignoreReturnValue) {
		if (function == null) {
			throw new IllegalArgumentException();
		}

		// Make sure the random number is unique
		long rand = 0;
		synchronized (this) {
			while (true) {
				ThreadUtil.sleep(1);
				rand = random.nextLong();

				if (!callMap.containsKey(rand) && !callQueue.contains(rand)) {
					break;
				}
			}
		}

		// Initialize struct
		FunctionWrapper wrapper = new FunctionWrapper();
		wrapper.id = rand;
		wrapper.function = function;
		wrapper.ignoreReturnValue = ignoreReturnValue;

//		System.out.println(Thread.currentThread().getName() + " -> " + thread.getName());
		if (thread.getId() == Thread.currentThread().getId()) {
			processFunction(wrapper);
			synchronized (this) {
				callMap.put(wrapper.id, wrapper);
			}
		} else {
			synchronized (this) {
				callQueue.add(wrapper);
				callMap.put(wrapper.id, wrapper);
			}
		}

		return rand;
	}

	// Quick queue

	public Object quickQueue(Function function) {
		long func = queue(function);
		return waitReturnValue(func);
	}

	public Object quickQueueVoid(VoidFunction function) {
		long func = queueVoid(function);
		return waitReturnValue(func);
	}

	public Object waitReturnValue(long funcId) {
		FunctionWrapper wrapper;
		synchronized (this) {
			wrapper = callMap.get(funcId);
		}
		if (wrapper.ignoreReturnValue) {
			return null;
		}
		while (!wrapper.finished) {
			ThreadUtil.sleep(1);
		}
		return getReturnValue(funcId);
	}

	public void waitFor(long funcId) {
		FunctionWrapper wrapper;
		synchronized (this) {
			wrapper = callMap.get(funcId);
		}
		while (!wrapper.finished) {
			ThreadUtil.sleep(1);
		}
		synchronized (this) {
			callMap.remove(funcId);
		}
	}

	public synchronized boolean hasFinished(long funcId) {
		return callMap.get(funcId).finished;
	}

	public synchronized boolean hasException(long funcId) {
		FunctionWrapper wrapper = callMap.get(funcId);
		return wrapper.finished && wrapper.exception != null;
	}

	public Object getReturnValue(long funcId) {
		FunctionWrapper wrapper;
		synchronized (this) {
			wrapper = callMap.get(funcId);
		}
		if(!wrapper.finished) {
			return null;
		}
		if (hasException(funcId)) {
			synchronized (this) {
				callMap.remove(funcId);
			}
			throw new DispatcherException(wrapper.exception);
		}
		synchronized (this) {
			callMap.remove(funcId);
		}
		return wrapper.returnValue;
	}

	public void computeEvents(int amount) {
		if (Thread.currentThread().getId() != thread.getId()) {
			return;
		}

		for (int i = 0; i < (int) MathUtil.min(amount, callQueue.size()); i++) {
			FunctionWrapper function;
			synchronized (this) {
				function = callQueue.remove(0);
			}
			processFunction(function);
		}
	}

	public void computeEvents() {
		computeEvents(Integer.MAX_VALUE);
	}

	private void processFunction(FunctionWrapper wrapper) {
		try {
			Object ret = wrapper.function.execute();
			if (!wrapper.ignoreReturnValue) {
				wrapper.returnValue = ret;
			}
		} catch (Throwable e) {
			if (!wrapper.ignoreReturnValue) {
				wrapper.exception = e;
			}
		} finally {
			wrapper.finished = true;
		}
	}

	public synchronized int getAmount() {
		return callQueue.size();
	}

	private static class FunctionWrapper {
		public long id;
		public Function function;
		public Object returnValue;
		public Throwable exception;
		public boolean ignoreReturnValue;
		public boolean finished;
	}

}
