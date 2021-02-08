package com.spaghettiengine.core;

import com.spaghettiengine.interfaces.*;
import com.spaghettiengine.utils.Logger;

public final class EventDispatcher {
	
	private IntentionHandler intentionHandler;
	
	// Signals are a kind of event that is handled LOCALLY on the server or client
	// and are never transmitted over network
	public void raiseSignal(GameObject issuer, long signal) {
		dispatchSignal(issuer, signal);
	}
	
	// Immediately dispatch signals because they are not transmitted over network
	public void dispatchSignal(GameObject issuer, long signal) {
		issuer.getLevel().forEachActualObject((id, object) -> {
			if(object instanceof SignalHandler) {
				SignalHandler cast = (SignalHandler) object;
				cast.handleSignal(issuer.getGame().isClient(), issuer, signal);
			}
		});
	}
	
	// Events can only be raised on the server side of a game and are transmitted to all clients
	// Events can also be caught by event handlers on both the server and the clients
	public void raiseEvent(GameObject issuer, GameEvent event) {
		if(!issuer.getGame().hasAuthority()) {
			throw new UnsupportedOperationException("You don't have authority to raise events (You must be a server or a singleplayer game instance)");
		}
		dispatchEvent(issuer, event);
	}
	
	// Dispatch signals before sending them over network and when they are received from network
	public void dispatchEvent(GameObject issuer, GameEvent event) {
		issuer.getLevel().forEachActualObject((id, object) -> {
			if(object instanceof EventHandler) {
				EventHandler cast = (EventHandler) object;
				cast.handleEvent(issuer.getGame().isClient(), issuer, event);
			}
		});
	}
	
	// Intentions can be raised by either client or server but are only handled on the other side GLOBALLY
	// They can be considered some kind of request from on side to another
	public void raiseIntention(GameObject issuer, long intention) {
		if(!issuer.getGame().isMultiplayer()) {
			throw new UnsupportedOperationException("You can't raise intentions in a singleplayer environment");
		}
	}
	
	// Dispatch intentions only when received 
	public void dispatchIntention(GameObject issuer, long intention) {
		if(intentionHandler == null) {
			Logger.warning(issuer.getGame(), "Intention (" + intention + ") received but no handler is registered");
		} else {
			intentionHandler.handleIntention(issuer.getGame().isClient(), issuer, intention);
		}
	}
	
	// Getter and setter
	
	public IntentionHandler getIntentionHandler() {
		return intentionHandler;
	}

	public void setIntentionHandler(IntentionHandler intentionHandler) {
		this.intentionHandler = intentionHandler;
	}
	
}