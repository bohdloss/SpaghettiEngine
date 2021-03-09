package com.spaghetti.networking;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.spaghetti.utils.Logger;

public final class NetworkBuffer {

	private static final byte b0 = (byte) 0, b1 = (byte) 1;
	private static final Charset UTF_16 = Charset.forName("UTF-16");

	protected ByteBuffer buffer;

	public NetworkBuffer(int size) {
		buffer = ByteBuffer.allocate(size);
	}

	// Getters and setters

	public void putDouble(double v) {
		buffer.putDouble(v);
	}

	public double getDouble() {
		return buffer.getDouble();
	}

	public void putInt(int v) {
		buffer.putInt(v);
	}

	public int getInt() {
		return buffer.getInt();
	}

	public void putShort(short v) {
		buffer.putShort(v);
	}

	public short getShort() {
		return buffer.getShort();
	}

	public void putLong(long v) {
		buffer.putLong(v);
	}

	public long getLong() {
		return buffer.getLong();
	}

	public void putFloat(float v) {
		buffer.putFloat(v);
	}

	public float getFloat() {
		return buffer.getFloat();
	}

	public void putChar(char v) {
		buffer.putChar(v);
	}

	public char getChar() {
		return buffer.getChar();
	}

	public void putByte(byte v) {
		buffer.put(v);
	}

	public byte getByte() {
		return buffer.get();
	}

	public void putBoolean(boolean v) {
		buffer.put(v ? b1 : b0);
	}

	public boolean getBoolean() {
		return buffer.get() != b0;
	}

	public void putString(String v) {
		byte[] array = v.getBytes(UTF_16);
		buffer.putInt(array.length);
		buffer.put(array);
	}

	public String getString() {
		int length = buffer.getInt();
		byte[] bytes = new byte[length];
		buffer.get(bytes, 0, length);
		String ret = new String(bytes, UTF_16);
		return ret;
	}

	public void putObject(Object obj) {
		if (obj instanceof Integer) {
			putInt((Integer) obj);
		} else if (obj instanceof Double) {
			putDouble((Double) obj);
		} else if (obj instanceof Short) {
			putShort((Short) obj);
		} else if (obj instanceof Long) {
			putLong((Long) obj);
		} else if (obj instanceof Float) {
			putFloat((Float) obj);
		} else if (obj instanceof Character) {
			putChar((Character) obj);
		} else if (obj instanceof Byte) {
			putByte((Byte) obj);
		} else if (obj instanceof Boolean) {
			putBoolean((Boolean) obj);
		} else if (obj instanceof String) {
			putString((String) obj);
		} else {
			putString(obj.toString());
		}
	}

	public void putObjects(Object... objects) {
		for (Object object : objects) {
			putObject(object);
		}
	}

	public void skip(int amount) {
		if (amount <= 0) {
			return;
		}
		buffer.position(buffer.position() + amount);
	}

	public int getSize() {
		return buffer.capacity();
	}

	public int getUsedSpace() {
		return buffer.position();
	}

	public int getFreeSpace() {
		return getSize() - getUsedSpace();
	}

}