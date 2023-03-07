package com.spaghetti.events;

public interface EventListener<T extends GameEvent> {

	@SuppressWarnings("unchecked")
	public default void handleEvent0(boolean isClient, GameEvent event) {
		handleEvent(isClient, (T) event);
	}

	public abstract void handleEvent(boolean isClient, T event);

}
