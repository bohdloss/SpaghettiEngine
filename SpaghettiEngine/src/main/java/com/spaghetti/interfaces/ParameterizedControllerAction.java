package com.spaghetti.interfaces;

import com.spaghetti.core.GameObject;

public interface ParameterizedControllerAction<T extends GameObject> extends ControllerAction<T> {

	public default void execute(T target) {
	}
	public abstract void execute(T target, Object...params);
	
}
