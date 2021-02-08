package com.spaghettiengine.interfaces;

import com.spaghettiengine.core.*;

public interface EventHandler {

	public abstract void handleEvent(boolean isClient, GameObject issuer, GameEvent event);

}
