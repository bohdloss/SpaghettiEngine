package com.spaghetti.networking;

import java.util.HashMap;

import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.joml.Vector4i;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;

public abstract class Serializer<T> {

	private static final HashMap<String, Serializer<?>> interpreters = new HashMap<>();

	@SuppressWarnings("unchecked")
	public static <U> Serializer<U> get(Class<U> cls) {
		return (Serializer<U>) interpreters.get(cls.getName());
	}

	public static <U> void register(Class<U> cls, Serializer<U> interpreter) {
		interpreters.put(cls.getName(), interpreter);
	}

	public static <U> void unregister(Class<U> cls) {
		interpreters.remove(cls.getName());
	}

	static {
		// Primitives
		interpreters.put("int", new Serializer<Integer>() {
			@Override
			public void writeClass(Integer object, NetworkBuffer buffer) {
				buffer.putInt(object);
			}

			@Override
			public Integer readClass(Integer original, NetworkBuffer buffer) {
				return buffer.getInt();
			}
		});
		interpreters.put("byte", new Serializer<Byte>() {
			@Override
			public void writeClass(Byte object, NetworkBuffer buffer) {
				buffer.putByte(object);
			}

			@Override
			public Byte readClass(Byte original, NetworkBuffer buffer) {
				return buffer.getByte();
			}
		});
		interpreters.put("short", new Serializer<Short>() {
			@Override
			public void writeClass(Short object, NetworkBuffer buffer) {
				buffer.putShort(object);
			}

			@Override
			public Short readClass(Short original, NetworkBuffer buffer) {
				return buffer.getShort();
			}
		});
		interpreters.put("long", new Serializer<Long>() {
			@Override
			public void writeClass(Long object, NetworkBuffer buffer) {
				buffer.putLong(object);
			}

			@Override
			public Long readClass(Long original, NetworkBuffer buffer) {
				return buffer.getLong();
			}
		});
		interpreters.put("char", new Serializer<Character>() {
			@Override
			public void writeClass(Character object, NetworkBuffer buffer) {
				buffer.putChar(object);
			}

			@Override
			public Character readClass(Character original, NetworkBuffer buffer) {
				return buffer.getChar();
			}
		});
		interpreters.put("float", new Serializer<Float>() {
			@Override
			public void writeClass(Float object, NetworkBuffer buffer) {
				buffer.putFloat(object);
			}

			@Override
			public Float readClass(Float original, NetworkBuffer buffer) {
				return buffer.getFloat();
			}
		});
		interpreters.put("double", new Serializer<Double>() {
			@Override
			public void writeClass(Double object, NetworkBuffer buffer) {
				buffer.putDouble(object);
			}

			@Override
			public Double readClass(Double original, NetworkBuffer buffer) {
				return buffer.getDouble();
			}
		});
		interpreters.put("boolean", new Serializer<Boolean>() {
			@Override
			public void writeClass(Boolean object, NetworkBuffer buffer) {
				buffer.putBoolean(object);
			}

			@Override
			public Boolean readClass(Boolean original, NetworkBuffer buffer) {
				return buffer.getBoolean();
			}
		});
		interpreters.put("java.lang.Integer", interpreters.get("int"));
		interpreters.put("java.lang.Byte", interpreters.get("byte"));
		interpreters.put("java.lang.Short", interpreters.get("short"));
		interpreters.put("java.lang.Long", interpreters.get("long"));
		interpreters.put("java.lang.Character", interpreters.get("char"));
		interpreters.put("java.lang.Float", interpreters.get("float"));
		interpreters.put("java.lang.Double", interpreters.get("double"));
		interpreters.put("java.lang.Boolean", interpreters.get("boolean"));
		interpreters.put("java.lang.String", new Serializer<String>() {
			@Override
			public void writeClass(String object, NetworkBuffer buffer) {
				buffer.putString(object);
			}

			@Override
			public String readClass(String original, NetworkBuffer buffer) {
				return buffer.getString();
			}
		});

		// Assets
		interpreters.put("com.spaghetti.assets.Asset", new Serializer<Asset>() {
			@Override
			public void writeClass(Asset object, NetworkBuffer buffer) {
				buffer.putString(object == null ? "" : object.getName());
			}

			@Override
			public Asset readClass(Asset original, NetworkBuffer buffer) {
				String asset_name = buffer.getString();
				return asset_name.equals("") ? null
						: Game.getInstance().getAssetManager().getAndLazyLoadAsset(asset_name);
			}
		});

		// Vector2
		interpreters.put("org.joml.Vector2d", new Serializer<Vector2d>() {
			@Override
			public void writeClass(Vector2d object, NetworkBuffer buffer) {
				buffer.putDouble(object.x);
				buffer.putDouble(object.y);
			}

			@Override
			public Vector2d readClass(Vector2d original, NetworkBuffer buffer) {
				Vector2d vec = original == null ? new Vector2d() : original;
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector2f", new Serializer<Vector2f>() {
			@Override
			public void writeClass(Vector2f object, NetworkBuffer buffer) {
				buffer.putFloat(object.x);
				buffer.putFloat(object.y);
			}

			@Override
			public Vector2f readClass(Vector2f original, NetworkBuffer buffer) {
				Vector2f vec = original == null ? new Vector2f() : original;
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector2i", new Serializer<Vector2i>() {
			@Override
			public void writeClass(Vector2i object, NetworkBuffer buffer) {
				buffer.putInt(object.x);
				buffer.putInt(object.y);
			}

			@Override
			public Vector2i readClass(Vector2i original, NetworkBuffer buffer) {
				Vector2i vec = original == null ? new Vector2i() : original;
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
				return vec;
			}
		});

		// Vector3
		interpreters.put("org.joml.Vector3d", new Serializer<Vector3d>() {
			@Override
			public void writeClass(Vector3d object, NetworkBuffer buffer) {
				buffer.putDouble(object.x);
				buffer.putDouble(object.y);
				buffer.putDouble(object.z);
			}

			@Override
			public Vector3d readClass(Vector3d original, NetworkBuffer buffer) {
				Vector3d vec = original == null ? new Vector3d() : original;
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
				vec.z = buffer.getDouble();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector3f", new Serializer<Vector3f>() {
			@Override
			public void writeClass(Vector3f object, NetworkBuffer buffer) {
				buffer.putFloat(object.x);
				buffer.putFloat(object.y);
				buffer.putFloat(object.z);
			}

			@Override
			public Vector3f readClass(Vector3f original, NetworkBuffer buffer) {
				Vector3f vec = original == null ? new Vector3f() : original;
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
				vec.z = buffer.getFloat();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector3i", new Serializer<Vector3i>() {
			@Override
			public void writeClass(Vector3i object, NetworkBuffer buffer) {
				buffer.putInt(object.x);
				buffer.putInt(object.y);
				buffer.putInt(object.z);
			}

			@Override
			public Vector3i readClass(Vector3i original, NetworkBuffer buffer) {
				Vector3i vec = original == null ? new Vector3i() : original;
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
				vec.z = buffer.getInt();
				return vec;
			}
		});

		// Vector4
		interpreters.put("org.joml.Vector4d", new Serializer<Vector4d>() {
			@Override
			public void writeClass(Vector4d object, NetworkBuffer buffer) {
				buffer.putDouble(object.x);
				buffer.putDouble(object.y);
				buffer.putDouble(object.z);
				buffer.putDouble(object.w);
			}

			@Override
			public Vector4d readClass(Vector4d original, NetworkBuffer buffer) {
				Vector4d vec = original == null ? new Vector4d() : original;
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
				vec.z = buffer.getDouble();
				vec.w = buffer.getDouble();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector4f", new Serializer<Vector4f>() {
			@Override
			public void writeClass(Vector4f object, NetworkBuffer buffer) {
				buffer.putFloat(object.x);
				buffer.putFloat(object.y);
				buffer.putFloat(object.z);
				buffer.putFloat(object.w);
			}

			@Override
			public Vector4f readClass(Vector4f original, NetworkBuffer buffer) {
				Vector4f vec = original == null ? new Vector4f() : original;
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
				vec.z = buffer.getFloat();
				vec.w = buffer.getFloat();
				return vec;
			}
		});
		interpreters.put("org.joml.Vector4i", new Serializer<Vector4i>() {
			@Override
			public void writeClass(Vector4i object, NetworkBuffer buffer) {
				buffer.putInt(object.x);
				buffer.putInt(object.y);
				buffer.putInt(object.z);
				buffer.putInt(object.w);
			}

			@Override
			public Vector4i readClass(Vector4i original, NetworkBuffer buffer) {
				Vector4i vec = original == null ? new Vector4i() : original;
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
				vec.z = buffer.getInt();
				vec.w = buffer.getInt();
				return vec;
			}
		});
	}

	public abstract void writeClass(T object, NetworkBuffer buffer);

	@SuppressWarnings("unchecked")
	public final void writeClassGeneric(Object object, NetworkBuffer buffer) {
		writeClass((T) object, buffer);
	}

	public abstract T readClass(T original, NetworkBuffer buffer);

	@SuppressWarnings("unchecked")
	public final Object readClassGeneric(Object original, NetworkBuffer buffer) {
		return readClass((T) original, buffer);
	}

}
