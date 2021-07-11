package com.spaghetti.interfaces;

import com.spaghetti.core.*;

public interface IntentionHandler {

	public abstract void handleIntention(boolean isClient, GameObject issuer, long intention);

}
