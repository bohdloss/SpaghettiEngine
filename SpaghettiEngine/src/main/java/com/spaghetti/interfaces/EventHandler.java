package com.spaghetti.interfaces;

import com.spaghetti.events.GameEvent;

public interface EventHandler {

	public abstract void handleEvent(boolean isClient, GameEvent event);

}
