package com.spaghetti.events;

import java.util.ArrayList;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.*;
import com.spaghetti.utils.*;

public class EventDispatcher {
	
	// Reference to owner
	protected final Game game;
	
	// Handlers
	protected ArrayList<SignalHandler> signalHandlers = new ArrayList<>();
	protected ArrayList<EventHandler> eventHandlers = new ArrayList<>();
	
	public EventDispatcher(Game game) {
		this.game = game;
	}

	// Signals are a kind of event that is handled LOCALLY on the server or client
	// and are never transmitted over network
	public void raiseSignal(long signal) {
		game.getUpdaterDispatcher().queue(() -> {
			dispatchSignal(signal);
			return null;
		}, true);
	}

	// Immediately dispatch signals because they are not transmitted over network
	protected void dispatchSignal(long signal) {
		if (signalHandlers.size() == 0) {
			Logger.info(game, "Signal (" + signal + ") received but no handler registered");
		} else {
			signalHandlers.forEach(handler -> {
				try {
					handler.handleSignal(game.isClient(), signal);
				} catch (Throwable t) {
					Logger.error("Error dispatching signal", t);
				}
			});
		}
	}

	// Events can be raised from both client and servers and are handled locally and
	// then sent with network
	// If an event is cancelled before being sent to the network queue, then it is
	// not sent
	public void raiseEvent(GameEvent event) {
		game.getUpdaterDispatcher().queue(() -> {
			event.setFrom(game.isClient() ? GameEvent.CLIENT : GameEvent.SERVER);
			dispatchEvent(event);
			if (!event.isCancelled()) {
				game.getNetworkManager().queueEvent(event);
			}
			return null;
		}, true);
	}

	// Dispatch events before sending them over network and when they are received
	// from network
	protected void dispatchEvent(GameEvent event) {
		game.getUpdaterDispatcher().queue(() -> {
			event.setFrom(game.isClient() ? GameEvent.SERVER : GameEvent.CLIENT);
	
			if (eventHandlers.size() == 0) {
				Logger.info(game,
						"Event " + event.getClass().getSimpleName() + " (from "
								+ (event.getFrom() == GameEvent.CLIENT ? "CLIENT" : "SERVER") + ", with id " + event.getId()
								+ ") received but no handler registered");
			} else {
				eventHandlers.forEach(handler -> {
					try {
						handler.handleEvent(game.isClient(), event);
					} catch (Throwable t) {
						Logger.error("Error dispatching event", t);
					}
				});
			}
			return null;
		}, true);
	}

	public void dispatchEvent(ConnectionManager.Identity identity, GameEvent event) {
		if (identity != null) {
			dispatchEvent(event);
		}
	}

	// Register global handlers

	public void registerSignalHandler(SignalHandler handler) {
		if (handler != null && !signalHandlers.contains(handler)) {
			signalHandlers.add(handler);
		}
	}

	public void unregisterSignalHandler(SignalHandler handler) {
		signalHandlers.remove(handler);
	}

	public void registerEventHandler(EventHandler handler) {
		if (handler != null && !eventHandlers.contains(handler)) {
			eventHandlers.add(handler);
		}
	}

	public void unregisterEventHandler(EventHandler handler) {
		eventHandlers.remove(handler);
	}

}