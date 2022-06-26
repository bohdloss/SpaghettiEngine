package com.spaghetti.networking;

import com.spaghetti.core.CoreComponent;
import com.spaghetti.core.GameComponent;
import com.spaghetti.core.GameObject;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.NetworkFunction;

public abstract class NetworkCore extends CoreComponent {

	// Handshake return codes
	public static final byte HUG = (byte) 0; // Success, client and server hug each other after handshake
	public static final byte INVALID_TOKEN = (byte) 1;
	public static final byte BANNED = (byte) 2;
	public static final byte REACHED_MAX = (byte) 3;
	public static final byte SNEAKED_IN = (byte) 4;
	public static final byte RECONNECTED = (byte) 5; // Success, but it's a reconnection attempt
	public static final byte GOODBYE = (byte) 6;
	public static final byte PING = (byte) 7;
	public static final byte KICKED = (byte) 8;
	public static final byte DATA = (byte) 9; // Indicates that this packet contains actual replication data
	public static final byte TOKEN = (byte) 10;

	// Abstract queue methods

	public abstract void queueNetworkFunction(NetworkFunction function);
	public abstract void queueEvent(GameEvent event);
	public abstract void queueRPC(RemoteProcedure rpc);
	public abstract void queueWriteLevel();
	public abstract void queueWriteData();
	public abstract void queueWriteObjectFull(GameObject obj);
	public abstract void queueWriteObjectTree(GameObject obj);
	public abstract void queueWriteObjectDestruction(GameObject obj);
	public abstract void queueWriteComponentDestruction(GameComponent comp);
	public abstract void queueWriteObject(GameObject obj);
	public abstract void queueWriteComponent(GameComponent comp);

}
