package com.spaghetti.networking;

public final class Opcode {

	private Opcode() {
	}

	public static final byte ITEM = (byte) 0;
	public static final byte STOP = (byte) 1;

	public static final byte LEVEL = (byte) 2;
	public static final byte GAMEOBJECT = (byte) 3;
	public static final byte GAMECOMPONENT = (byte) 4;

	public static final byte INTENTION = (byte) 5;
	public static final byte GAMEEVENT = (byte) 6;

	public static final byte CAMERA = (byte) 7;
	public static final byte CONTROLLER = (byte) 8;

	public static final byte END = (byte) 9;

}
