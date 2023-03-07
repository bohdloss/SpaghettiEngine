package com.spaghetti.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.spaghetti.core.Game;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.utils.Logger;

public class EventDispatcher {

	// Reference to owner
	protected final Game game;

	// Handlers
	protected Map<Class<?>, List<EventListener<?>>> eventListeners = new HashMap<>();

	public EventDispatcher(Game game) {
		this.game = game;
	}

	/**
	 * Raises an event which in turn notifies the respective handlers
	 *
	 * Events can be raised from both client and servers and are handled locally and
	 * then sent to the network event queue
	 * If an event is cancelled before being sent to the network queue, then it is
	 * not sent
	 *
	 * @param event
	 */
	public void raiseEvent(GameEvent event) {
		game.getUpdaterDispatcher().quickQueueVoid(() -> {
			event.setFrom(game.isClient() ? GameEvent.CLIENT : GameEvent.SERVER);
			dispatchEvent(event);
			if (!event.isCancelled() && !event.isLocal()) {
				game.getNetworkManager().queueEvent(event);
			}
		});
	}

	// Dispatch events before sending them over network and when they are received
	// from network
	protected void dispatchEvent(GameEvent event) {
		game.getUpdaterDispatcher().queueVoid(() -> {
			event.setFrom(game.isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

			/*Logger.info(game,
					"Event " + event.getClass().getSimpleName() + " (from "
							+ (event.getFrom() == GameEvent.CLIENT ? "CLIENT" : "SERVER") + ", with id " + event.getId()
							+ ") received but no listener registered");*/

			eventListeners.forEach((cls, listenerList) -> {
				if(cls.isAssignableFrom(event.getClass())) {
					listenerList.forEach(handler -> {
						try {
							handler.handleEvent0(game.isClient(), event);
						} catch (Throwable t) {
							Logger.error("Error dispatching event", t);
						}
					});
				}
			});
		}, true);
	}

	public void dispatchEvent(ConnectionManager.Identity identity, GameEvent event) {
		if (identity != null) {
			dispatchEvent(event);
		}
	}

	// Register global handlers

	public <T extends GameEvent> void registerEventListener(Class<T> cls, EventListener<T> listener) {
		if (listener != null) {
			List<EventListener<?>> listenerList = eventListeners.get(cls);

			if(listenerList == null) {
				List<EventListener<?>> newList = new ArrayList<>(1);
				listenerList = newList;
				eventListeners.put(cls, newList);
			}

			listenerList.add(listener);
		}
	}

	public <T extends GameEvent> void unregisterEventListener(Class<T> cls, EventListener<T> listener) {
		if(listener != null) {
			List<EventListener<?>> listenerList = eventListeners.get(cls);

			if(listenerList != null) {
				listenerList.remove(listener);
			}
		}
	}

}