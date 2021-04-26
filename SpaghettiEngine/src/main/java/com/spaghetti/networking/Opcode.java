package com.spaghetti.networking;

public final class Opcode {

	private Opcode() {
	}

	public static final byte ITEM = (byte) 99;
	public static final byte STOP = (byte) 88;

	public static final byte LEVEL = (byte) 2;
	public static final byte GAMEOBJECT = (byte) 3;
	public static final byte GAMECOMPONENT = (byte) 4;

	public static final byte INTENTION = (byte) 5;
	public static final byte GAMEEVENT = (byte) 6;

	public static final byte CAMERA = (byte) 7;
	public static final byte CONTROLLER = (byte) 8;
	public static final byte PLAYER = (byte) 12;

	public static final byte END = (byte) 9;

	public static final byte DATA = (byte) 10;

	public static final byte PINGPONG = (byte) 11;

	public static final byte OBJFUNC_REPARENT = (byte) 13;
	public static final byte OBJFUNC_ORPHAN = (byte) 14;
	public static final byte OBJFUNC_DESTROY = (byte) 15;
	public static final byte OBJFUNC_CREATE = (byte) 16;

	public static final byte COMPFUNC_REPARENT = (byte) 17;
	public static final byte COMPFUNC_ORPHAN = (byte) 18;
	public static final byte COMPFUNC_DESTROY = (byte) 19;
	public static final byte COMPFUNC_CREATE = (byte) 20;

}
