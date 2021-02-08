package com.spaghettiengine.interfaces;

import com.spaghettiengine.core.*;

public interface IntentionHandler {

	public abstract void handleIntention(boolean isClient, GameObject issuer, long intention);
	
}
