package com.spaghetti.networking;

public final class Opcode {

	private Opcode() {
	}

	public static final byte ITEM = (byte) 99;
	public static final byte STOP = (byte) 88;

	public static final byte LEVEL = (byte) 2;
	public static final byte GAMEOBJECT = (byte) 3;
	public static final byte GAMECOMPONENT = (byte) 4;

	public static final byte GAMEEVENT = (byte) 6;

	public static final byte CAMERA = (byte) 7;
	public static final byte PLAYER = (byte) 9;

	public static final byte END = (byte) 10;

	public static final byte DATA = (byte) 11;

	public static final byte PING_PONG = (byte) 12;

	public static final byte REMOTEPROCEDURE = (byte) 13;
	public static final byte RP_RESPONSE = (byte) 14;
	public static final byte RP_ACKNOWLEDGEMENT = (byte) 15;

	public static final byte OBJECT_TREE = (byte) 16;
	public static final byte OBJECT_DESTROY = (byte) 17;

	public static final byte GOODBYE = (byte) 18;

}
