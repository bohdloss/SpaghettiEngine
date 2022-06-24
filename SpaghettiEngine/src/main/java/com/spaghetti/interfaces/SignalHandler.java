package com.spaghetti.interfaces;

import com.spaghetti.core.GameObject;

public interface SignalHandler {

	public abstract void handleSignal(boolean isClient, long signal);

}
