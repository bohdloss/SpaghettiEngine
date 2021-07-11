package com.spaghetti.networking;

import java.util.HashMap;

import org.joml.*;

import com.spaghetti.interfaces.ClassInterpreter;
import com.spaghetti.assets.*;

public final class DefaultInterpreters {

	private DefaultInterpreters() {
	}

	public static final HashMap<String, ClassInterpreter<?>> interpreters = new HashMap<>();

	static {
		// Primitives
		interpreters.put("int", new ClassInterpreter<Integer>() {
			@Override
			public void writeClass(Integer object, NetworkBuffer buffer) {
				buffer.putInt(object);
			}

			@Override
			public Integer readClass(Integer original, NetworkBuffer buffer) {
				return buffer.getInt();
			}
		});
		interpreters.put("byte", new ClassInterpreter<Byte>() {
			@Override
			public void writeClass(Byte object, NetworkBuffer buffer) {
				buffer.putByte(object);
			}

			@Override
			public Byte readClass(Byte original, NetworkBuffer buffer) {
				return buffer.getByte();
			}
		});
		interpreters.put("short", new ClassInterpreter<Short>() {
			@Override
			public void writeClass(Short object, NetworkBuffer buffer) {
				buffer.putShort(object);
			}

			@Override
			public Short readClass(Short original, NetworkBuffer buffer) {
				return buffer.getShort();
			}
		});
		interpreters.put("long", new ClassInterpreter<Long>() {
			@Override
			public void writeClass(Long object, NetworkBuffer buffer) {
				buffer.putLong(object);
			}

			@Override
			public Long readClass(Long original, NetworkBuffer buffer) {
				return buffer.getLong();
			}
		});
		interpreters.put("char", new ClassInterpreter<Character>() {
			@Override
			public void writeClass(Character object, NetworkBuffer buffer) {
				buffer.putChar(object);
			}

			@Override
			public Character readClass(Character original, NetworkBuffer buffer) {
				return buffer.getChar();
			}
		});
		interpreters.put("float", new ClassInterpreter<Float>() {
			@Override
			public void writeClass(Float object, NetworkBuffer buffer) {
				buffer.putFloat(object);
			}

			@Override
			public Float readClass(Float original, NetworkBuffer buffer) {
				return buffer.getFloat();
			}
		});
		interpreters.put("double", new ClassInterpreter<Double>() {
			@Override
			public void writeClass(Double object, NetworkBuffer buffer) {
				buffer.putDouble(object);
			}

			@Override
			public Double readClass(Double original, NetworkBuffer buffer) {
				return buffer.getDouble();
			}
		});
		interpreters.put("boolean", new ClassInterpreter<Boolean>() {
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
		interpreters.put("java.lang.String", new ClassInterpreter<String>() {
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
		interpreters.put("com.spaghetti.assets.Asset", new ClassInterpreter<Asset>() {
			@Override
			public void writeClass(Asset object, NetworkBuffer buffer) {
				buffer.putString(object == null ? "" : object.getName());
			}

			@Override
			public Asset readClass(Asset original, NetworkBuffer buffer) {
				String asset_name = buffer.getString();
				return asset_name.equals("") ? null : buffer.getWorker().getGame().getAssetManager().custom(asset_name);
			}
		});

		// Vector2
		interpreters.put("org.joml.Vector2d", new ClassInterpreter<Vector2d>() {
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
		interpreters.put("org.joml.Vector2f", new ClassInterpreter<Vector2f>() {
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
		interpreters.put("org.joml.Vector2i", new ClassInterpreter<Vector2i>() {
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
		interpreters.put("org.joml.Vector3d", new ClassInterpreter<Vector3d>() {
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
		interpreters.put("org.joml.Vector3f", new ClassInterpreter<Vector3f>() {
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
		interpreters.put("org.joml.Vector3i", new ClassInterpreter<Vector3i>() {
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
		interpreters.put("org.joml.Vector4d", new ClassInterpreter<Vector4d>() {
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
		interpreters.put("org.joml.Vector4f", new ClassInterpreter<Vector4f>() {
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
		interpreters.put("org.joml.Vector4i", new ClassInterpreter<Vector4i>() {
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

}
