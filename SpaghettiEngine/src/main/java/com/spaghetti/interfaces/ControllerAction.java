package com.spaghetti.interfaces;

import com.spaghetti.core.GameObject;

public interface ControllerAction<T extends GameObject> {

	public abstract void execute(T target);
	public default void execute(T target, Object...params) {
		execute(target);
	}
	
}
