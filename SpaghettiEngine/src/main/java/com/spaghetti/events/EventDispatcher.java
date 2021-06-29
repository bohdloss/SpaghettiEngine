package com.spaghetti.events;

import java.util.ArrayList;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.*;
import com.spaghetti.utils.*;

public class EventDispatcher {

	protected final Game game;

	protected ArrayList<SignalHandler> sig_ghandles = new ArrayList<>();
	protected ArrayList<EventHandler> evnt_ghandles = new ArrayList<>();
	protected ArrayList<IntentionHandler> int_ghandles = new ArrayList<>();

	public EventDispatcher(Game game) {
		this.game = game;
	}

	// Signals are a kind of event that is handled LOCALLY on the server or client
	// and are never transmitted over network
	public void raiseSignal(GameObject issuer, long signal) {
		dispatchSignal(issuer, signal);
	}

	// Immediately dispatch signals because they are not transmitted over network
	protected void dispatchSignal(GameObject issuer, long signal) {
		if (sig_ghandles.size() == 0) {
			Logger.info(game, "Signal (" + signal + ") received but no handler registered");
		} else {
			sig_ghandles.forEach(handler -> {
				try {
					handler.handleSignal(game.isClient(), issuer, signal);
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
	public void raiseEvent(GameObject issuer, GameEvent event) {
		event.setFrom(game.isClient() ? GameEvent.CLIENT : GameEvent.SERVER);
		dispatchEvent(issuer, event);
		if (event.isCancelled()) {
			return;
		}
		if (game.getClient() != null) {
			ClientCore client = game.getClient();
			client.queueEvent(issuer, event);
		} else {
			ServerCore server = game.getServer();
			server.queueEvent(issuer, event);
		}
	}

	// Dispatch events before sending them over network and when they are received
	// from network
	protected void dispatchEvent(GameObject issuer, GameEvent event) {
		event.setFrom(game.isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

		if (evnt_ghandles.size() == 0) {
			Logger.info(game, "Event " + event.getClass().getSimpleName() + " (from " + (event.getFrom() == GameEvent.CLIENT ? "CLIENT" : "SERVER")
					+ ", with id " + event.getId() + ") received but no handler registered");
		} else {
			evnt_ghandles.forEach(handler -> {
				try {
					handler.handleEvent(game.isClient(), issuer, event);
				} catch (Throwable t) {
					Logger.error("Error dispatching event", t);
				}
			});
		}
	}

	public void dispatchEvent(NetworkConnection.Identity identity, GameObject issuer, GameEvent event) {
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
		if (game.getClient() != null) {
			ClientCore client = game.getClient();
			client.queueIntention(issuer, intention);
		} else {
			ServerCore server = game.getServer();
			server.queueIntention(issuer, intention);
		}
	}

	// Dispatch intentions only when received
	protected void dispatchIntention(GameObject issuer, long intention) {
		if (int_ghandles.size() == 0) {
			Logger.warning(game, "Intention (" + intention + ") received but no handler is registered");
		} else {
			int_ghandles.forEach(handler -> {
				try {
					handler.handleIntention(game.isClient(), issuer, intention);
				} catch (Throwable t) {
					Logger.error("Error dispatching intention", t);
				}
			});
		}
	}

	public void dispatchIntention(NetworkConnection.Identity identity, GameObject issuer, long intention) {
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