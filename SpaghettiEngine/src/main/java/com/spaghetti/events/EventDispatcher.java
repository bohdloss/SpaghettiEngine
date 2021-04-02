package com.spaghetti.events;

import java.util.ArrayList;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.*;
import com.spaghetti.utils.*;

public final class EventDispatcher {

	private final Game game;

	private ArrayList<SignalHandler> sig_ghandles = new ArrayList<>();
	private ArrayList<EventHandler> evnt_ghandles = new ArrayList<>();
	private ArrayList<IntentionHandler> int_ghandles = new ArrayList<>();

	public EventDispatcher(Game game) {
		this.game = game;
	}

	// Signals are a kind of event that is handled LOCALLY on the server or client
	// and are never transmitted over network
	public void raiseSignal(GameObject issuer, long signal) {
		dispatchSignal(issuer, signal);
	}

	// Immediately dispatch signals because they are not transmitted over network
	private void dispatchSignal(GameObject issuer, long signal) {
		if (sig_ghandles.size() == 0) {
			Logger.info(game, "Signal (" + signal + ") received but no joinHandler registered");
		} else {
			sig_ghandles.forEach(handler -> {
				handler.handleSignal(game.isClient(), issuer, signal);
			});
		}
	}

	// Events can be raised from both client and servers and are handled locally and
	// then sent with network
	// If an event is cancelled before being sent to the network queue, then it is
	// not sent
	public void raiseEvent(GameObject issuer, GameEvent event) {
		event.setFrom(game.isClient() ? GameEvent.CLIENT : GameEvent.SERVER);
		dispatchEvent(issuer, event);
	}

	// Dispatch signals before sending them over network and when they are received
	// from network
	private void dispatchEvent(GameObject issuer, GameEvent event) {
		event.setFrom(game.isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

		if (evnt_ghandles.size() == 0) {
			Logger.info(game, "Event (" + (event.getFrom() == GameEvent.CLIENT ? "CLIENT" : "SERVER") + ", "
					+ event.getId() + ") received but no joinHandler registered");
		} else {
			evnt_ghandles.forEach(handler -> {
				handler.handleEvent(game.isClient(), issuer, event);
			});
		}
	}

	public void dispatchEvent(NetworkWorker.Identity identity, GameObject issuer, GameEvent event) {
		if (identity != null) {
			dispatchEvent(issuer, event);
		}
	}

	// Intentions can be raised by either client or server but are only handled on
	// the other side GLOBALLY
	// They can be considered some kind of request from on side to another
	public void raiseIntention(GameObject issuer, long intention) {
		if (!game.isMultiplayer()) {
			throw new UnsupportedOperationException("You can't raise intentions in a singleplayer environment");
		}
	}

	// Dispatch intentions only when received
	private void dispatchIntention(GameObject issuer, long intention) {
		if (int_ghandles.size() == 0) {
			Logger.warning(game, "Intention (" + intention + ") received but no joinHandler is registered");
		} else {
			int_ghandles.forEach(handler -> {
				handler.handleIntention(game.isClient(), issuer, intention);
			});
		}
	}

	public void dispatchIntention(NetworkWorker.Identity identity, GameObject issuer, long intention) {
		if (identity != null) {
			dispatchIntention(issuer, intention);
		}
	}

	// Register global handlers

	public void registerSignalHandler(SignalHandler handler) {
		if (handler != null && !sig_ghandles.contains(handler)) {
			sig_ghandles.add(handler);
		}
	}

	public void unregisterSignalHandler(SignalHandler handler) {
		sig_ghandles.remove(handler);
	}

	public void registerEventHandler(EventHandler handler) {
		if (handler != null && !evnt_ghandles.contains(handler)) {
			evnt_ghandles.add(handler);
		}
	}

	public void unregisterEventHandler(EventHandler handler) {
		evnt_ghandles.remove(handler);
	}

	public void registerIntentionHandler(IntentionHandler handler) {
		if (handler != null && !int_ghandles.contains(handler)) {
			int_ghandles.add(handler);
		}
	}

	public void unregisterIntentionHandler(IntentionHandler handler) {
		int_ghandles.remove(handler);
	}

}