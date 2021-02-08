package com.spaghettiengine.interfaces;

import com.spaghettiengine.core.GameObject;

public interface SignalHandler {

	public abstract void handleSignal(boolean isClient, GameObject issuer, long signal);

}
