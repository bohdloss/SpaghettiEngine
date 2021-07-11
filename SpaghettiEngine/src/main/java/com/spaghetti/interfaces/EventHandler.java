package com.spaghetti.interfaces;

import com.spaghetti.core.*;
import com.spaghetti.events.GameEvent;

public interface EventHandler {

	public abstract void handleEvent(boolean isClient, GameObject issuer, GameEvent event);

}
