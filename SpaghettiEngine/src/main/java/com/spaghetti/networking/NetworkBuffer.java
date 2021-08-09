package com.spaghetti.networking;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.spaghetti.utils.Utils;

public final class NetworkBuffer {

	public static final byte b0 = (byte) 0, b1 = (byte) 1;
	public static final Charset UTF_16 = Charset.forName("UTF-16");
	public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
	public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
	public static final Charset UTF_8 = Charset.forName("UTF-8");
	public static final Charset US_ASCII = Charset.forName("US-ASCII");
	public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
	public static final ByteOrder ORDER = ByteOrder.BIG_ENDIAN;

	protected ByteBuffer buffer;
	protected final NetworkConnection owner;

	public NetworkBuffer(NetworkConnection owner, int size) {
		this.owner = owner;
		buffer = ByteBuffer.allocate(size);
		buffer.order(ORDER);
	}

	// Getters and setters

	// Double
	public void putDouble(double v) {
		buffer.putDouble(v);
	}

	public void putDoubleAt(int index, double v) {
		buffer.putDouble(index, v);
	}

	public double getDouble() {
		return buffer.getDouble();
	}

	public double getDoubleAt(int index) {
		return buffer.getDouble(index);
	}

	// Int
	public void putInt(int v) {
		buffer.putInt(v);
	}

	public void putIntAt(int index, int v) {
		buffer.putInt(index, v);
	}

	public int getInt() {
		return buffer.getInt();
	}

	public int getIntAt(int index) {
		return buffer.getInt(index);
	}

	// Short
	public void putShort(short v) {
		buffer.putShort(v);
	}

	public void putShortAt(int index, short v) {
		buffer.putShort(index, v);
	}

	public short getShort() {
		return buffer.getShort();
	}

	public short getShortAt(int index) {
		return buffer.getShort(index);
	}

	// Long
	public void putLong(long v) {
		buffer.putLong(v);
	}

	public void putLongAt(int index, long v) {
		buffer.putLong(index, v);
	}

	public long getLong() {
		return buffer.getLong();
	}

	public long getLongAt(int index) {
		return buffer.getLong(index);
	}

	// Float
	public void putFloat(float v) {
		buffer.putFloat(v);
	}

	public void putFloatAt(int index, float v) {
		buffer.putFloat(index, v);
	}

	public float getFloat() {
		return buffer.getFloat();
	}

	public float getFloatAt(int index) {
		return buffer.getFloat(index);
	}

	// Char
	public void putChar(char v) {
		buffer.putChar(v);
	}

	public void putCharAt(int index, char v) {
		buffer.putChar(index, v);
	}

	public char getChar() {
		return buffer.getChar();
	}

	public char getCharAt(int index) {
		return buffer.getChar(index);
	}

	// Byte
	public void putByte(byte v) {
		buffer.put(v);
	}

	public void putByteAt(int index, byte v) {
		buffer.put(index, v);
	}

	public byte getByte() {
		return buffer.get();
	}

	public byte getByteAt(int index) {
		return buffer.get(index);
	}

	public void putBytes(byte[] buf, int buf_offset, int amount) {
		buffer.put(buf, buf_offset, amount);
	}

	public void putBytesAt(int index, byte[] buf, int buf_offset, int amount) {
		int position = buffer.position();
		try {
			buffer.position(index);
			putBytes(buf, buf_offset, amount);
		} finally {
			buffer.position(position);
		}
	}

	public void putBytes(byte[] buf) {
		putBytes(buf, 0, buf.length);
	}

	public void putBytesAt(int index, byte[] buf) {
		putBytesAt(index, buf, 0, buf.length);
	}

	public void getBytes(byte[] buf, int buf_offset, int amount) {
		buffer.get(buf, buf_offset, amount);
	}

	public void getBytesAt(int index, byte[] buf, int buf_offset, int amount) {
		int position = buffer.position();
		try {
			buffer.position(index);
			getBytes(buf, buf_offset, amount);
		} finally {
			buffer.position(position);
		}
	}

	public void getBytes(byte[] buf) {
		getBytes(buf, 0, buf.length);
	}

	public void getBytesAt(int index, byte[] buf) {
		getBytesAt(index, buf, 0, buf.length);
	}

	// Boolean
	public void putBoolean(boolean v) {
		buffer.put(v ? b1 : b0);
	}

	public void putBooleanAt(int index, boolean v) {
		buffer.put(index, v ? b1 : b0);
	}

	public boolean getBoolean() {
		return buffer.get() != b0;
	}

	public boolean getBooleanAt(int index) {
		return buffer.get(index) != b0;
	}

	// String
	public void putString(boolean cache, String v, Charset charset) {
		if (cache) {
			short hash = Utils.shortHash(v);
			buffer.putShort(hash);
			if (!owner.str_cache.containsKey(hash)) {
				owner.str_cache.put(hash, v);
			} else {
				return;
			}
		}
		byte[] array = v.getBytes(charset);
		buffer.putShort((short) array.length);
		buffer.put(array);
	}

	public void putString(String v, Charset charset) {
		putString(false, v, charset);
	}

	public void putStringAt(int index, boolean cache, String v, Charset charset) {
		int position = buffer.position();
		try {
			buffer.position(index);
			putString(cache, v, charset);
		} finally {
			buffer.position(position);
		}
	}

	public void putStringAt(int index, String v, Charset charset) {
		putStringAt(index, false, v, charset);
	}

	public void putString(String v) {
		putString(v, UTF_8);
	}

	public void putStringAt(int index, String v) {
		putStringAt(index, v, UTF_8);
	}

	public String getString(boolean cache, Charset charset) {
		if (cache) {
			short hash = buffer.getShort();
			if (!owner.str_cache.containsKey(hash)) {
				short length = buffer.getShort();
				byte[] bytes = new byte[length];
				buffer.get(bytes, 0, length);
				String str = new String(bytes, charset);
				owner.str_cache.put(hash, str);
				return str;
			} else {
				return owner.str_cache.get(hash);
			}
		}
		short length = buffer.getShort();
		byte[] bytes = new byte[length];
		buffer.get(bytes, 0, length);
		String ret = new String(bytes, charset);
		return ret;
	}

	public String getString(Charset charset) {
		return getString(false, charset);
	}

	public String getStringAt(int index, boolean cache, Charset charset) {
		int position = buffer.position();
		try {
			buffer.position(index);
			return getString(cache, charset);
		} finally {
			buffer.position(position);
		}
	}

	public String getStringAt(int index, Charset charset) {
		return getStringAt(index, false, charset);
	}

	public String getString() {
		return getString(UTF_8);
	}

	public String getStringAt(int index) {
		return getStringAt(index, UTF_8);
	}

	// Generic
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

	public void putObjectAt(int index, Object obj) {
		int position = buffer.position();
		try {
			putObject(obj);
		} finally {
			buffer.position(position);
		}
	}

	public void putObjects(Object... objects) {
		for (Object object : objects) {
			putObject(object);
		}
	}

	public void putObjectsAt(int index, Object... objects) {
		int position = buffer.position();
		try {
			putObjects(objects);
		} finally {
			buffer.position(position);
		}
	}

	// Utility

	public NetworkConnection getConnection() {
		return owner;
	}

	public void skip(int amount) {
		buffer.position(buffer.position() + amount);
	}

	public int getSize() {
		return buffer.capacity();
	}

	public int getUsedSpace() {
		return buffer.position();
	}

	public int getFreeSpace() {
		return buffer.remaining();
	}

	public int getPosition() {
		return buffer.position();
	}

	public void setPosition(int position) {
		buffer.position(position);
	}

	public int getLimit() {
		return buffer.limit();
	}

	public void setLimit(int limit) {
		buffer.limit(limit);
	}

	public byte[] asArray() {
		return buffer.array();
	}

	public void clear() {
		buffer.clear();
	}

	public void flip() {
		buffer.flip();
	}

	public void rewind() {
		buffer.rewind();
	}

	public void mark() {
		buffer.mark();
	}

	public void empty() {
		byte[] array = buffer.array();
		for (int i = 0; i < array.length; i++) {
			array[i] = 0;
		}
	}

	public ByteOrder getByteOrder() {
		return buffer.order();
	}

	public void setByteOrder(ByteOrder order) {
		buffer.order(order);
	}

	public ByteBuffer getRaw() {
		return buffer;
	}

}