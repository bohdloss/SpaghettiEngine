package com.spaghetti.input;

import com.spaghetti.world.GameObject;

public interface ControllerAction<T extends GameObject> {

	public abstract void execute(T target);

}
