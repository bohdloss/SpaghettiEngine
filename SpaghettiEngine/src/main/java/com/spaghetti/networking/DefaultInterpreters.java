package com.spaghetti.networking;

import java.lang.reflect.Field;
import java.util.HashMap;

import org.joml.*;

import com.spaghetti.interfaces.ClassInterpreter;
import com.spaghetti.assets.*;
import com.spaghetti.core.Game;

public final class DefaultInterpreters {

	private DefaultInterpreters() {
	}

	public static final HashMap<String, ClassInterpreter> interpreters = new HashMap<>();

	static {
		// Primitives
		interpreters.put("int", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putInt(field.getInt(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getInt());
			}
		});
		interpreters.put("byte", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putByte(field.getByte(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getByte());
			}
		});
		interpreters.put("short", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putShort(field.getShort(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getShort());
			}
		});
		interpreters.put("long", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putLong(field.getLong(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getLong());
			}
		});
		interpreters.put("char", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putChar(field.getChar(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getChar());
			}
		});
		interpreters.put("float", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putFloat(field.getFloat(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getFloat());
			}
		});
		interpreters.put("double", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putDouble(field.getDouble(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getDouble());
			}
		});
		interpreters.put("boolean", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putBoolean(field.getBoolean(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getBoolean());
			}
		});
		interpreters.put("Integer", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putInt(field.getInt(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getInt());
			}
		});
		interpreters.put("Byte", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putByte(field.getByte(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getByte());
			}
		});
		interpreters.put("Short", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putShort(field.getShort(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getShort());
			}
		});
		interpreters.put("Long", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putLong(field.getLong(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getLong());
			}
		});
		interpreters.put("Character", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putChar(field.getChar(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getChar());
			}
		});
		interpreters.put("Float", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putFloat(field.getFloat(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getFloat());
			}
		});
		interpreters.put("Double", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putDouble(field.getDouble(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getDouble());
			}
		});
		interpreters.put("Boolean", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putBoolean(field.getBoolean(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getBoolean());
			}
		});
		interpreters.put("Long", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putLong(field.getLong(object));
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				field.set(object, buffer.getLong());
			}
		});

		// Assets
		interpreters.put("com.spaghetti.assets.Asset", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				buffer.putString(((Asset) field.get(object)).getName());
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				AssetManager asset_manager = Game.getGame().getAssetManager();
				String asset_name = buffer.getString();
				Asset asset = asset_manager.custom(asset_name);
				field.set(object, asset);
			}
		});

		// Vector2
		interpreters.put("org.joml.Vector2d", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2d vec = (Vector2d) field.get(object);
				buffer.putDouble(vec.x);
				buffer.putDouble(vec.y);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2d vec = (Vector2d) field.get(object);
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
			}
		});
		interpreters.put("org.joml.Vector2f", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2f vec = (Vector2f) field.get(object);
				buffer.putFloat(vec.x);
				buffer.putFloat(vec.y);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2f vec = (Vector2f) field.get(object);
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
			}
		});
		interpreters.put("org.joml.Vector2i", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2i vec = (Vector2i) field.get(object);
				buffer.putInt(vec.x);
				buffer.putInt(vec.y);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector2i vec = (Vector2i) field.get(object);
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
			}
		});

		// Vector3
		interpreters.put("org.joml.Vector3d", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3d vec = (Vector3d) field.get(object);
				buffer.putDouble(vec.x);
				buffer.putDouble(vec.y);
				buffer.putDouble(vec.z);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3d vec = (Vector3d) field.get(object);
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
				vec.z = buffer.getDouble();
			}
		});
		interpreters.put("org.joml.Vector3f", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3f vec = (Vector3f) field.get(object);
				buffer.putFloat(vec.x);
				buffer.putFloat(vec.y);
				buffer.putFloat(vec.z);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3f vec = (Vector3f) field.get(object);
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
				vec.z = buffer.getFloat();
			}
		});
		interpreters.put("org.joml.Vector3i", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3i vec = (Vector3i) field.get(object);
				buffer.putInt(vec.x);
				buffer.putInt(vec.y);
				buffer.putInt(vec.z);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector3i vec = (Vector3i) field.get(object);
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
				vec.z = buffer.getInt();
			}
		});

		// Vector4
		interpreters.put("org.joml.Vector4d", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4d vec = (Vector4d) field.get(object);
				buffer.putDouble(vec.x);
				buffer.putDouble(vec.y);
				buffer.putDouble(vec.z);
				buffer.putDouble(vec.w);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4d vec = (Vector4d) field.get(object);
				vec.x = buffer.getDouble();
				vec.y = buffer.getDouble();
				vec.z = buffer.getDouble();
				vec.w = buffer.getDouble();
			}
		});
		interpreters.put("org.joml.Vector4f", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4f vec = (Vector4f) field.get(object);
				buffer.putFloat(vec.x);
				buffer.putFloat(vec.y);
				buffer.putFloat(vec.z);
				buffer.putFloat(vec.w);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4f vec = (Vector4f) field.get(object);
				vec.x = buffer.getFloat();
				vec.y = buffer.getFloat();
				vec.z = buffer.getFloat();
				vec.w = buffer.getFloat();
			}
		});
		interpreters.put("org.joml.Vector4i", new ClassInterpreter() {
			@Override
			public void writeClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4i vec = (Vector4i) field.get(object);
				buffer.putInt(vec.x);
				buffer.putInt(vec.y);
				buffer.putInt(vec.z);
				buffer.putInt(vec.w);
			}

			@Override
			public void readClass(Field field, Object object, NetworkBuffer buffer) throws IllegalAccessException {
				Vector4i vec = (Vector4i) field.get(object);
				vec.x = buffer.getInt();
				vec.y = buffer.getInt();
				vec.z = buffer.getInt();
				vec.w = buffer.getInt();
			}
		});
	}

}
