package com.spaghetti.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import org.joml.*;

import com.spaghetti.assets.Asset;
import com.spaghetti.assets.AssetManager;
import com.spaghetti.core.*;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.GameEvent;
import com.spaghetti.interfaces.*;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.Logger;

public class NetworkWorker {

	public static final class Identity {
		private Identity() {
		}
	}

	protected static final Identity IDENTITY = new Identity();

	// Data
	protected final CoreComponent owner;
	protected NetworkBuffer w_buffer;
	protected NetworkBuffer r_buffer;
	protected Socket socket;
	protected Authenticator authenticator;

	protected byte[] length_b = new byte[4];

	// Reflection
	protected static final Field f_oid;
	protected static final Field f_cid;
	protected static final Field f_eid;
	protected static final Field f_modifiers;
	protected static final HashMap<String, Class<?>> m_clss = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameObject>> m_oconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameComponent>> m_cconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameEvent>> m_econstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Field[]> m_fields = new HashMap<>();
	protected static final HashMap<Class<?>, Boolean> m_noreplicate = new HashMap<>();

	static {
		Field o = null;
		Field c = null;
		Field ev = null;
		Field mod = null;
		try {
			o = GameObject.class.getDeclaredField("id");
			o.setAccessible(true);
			c = GameComponent.class.getDeclaredField("id");
			c.setAccessible(true);
			ev = GameEvent.class.getDeclaredField("id");
			ev.setAccessible(true);
			mod = Field.class.getDeclaredField("modifiers"); // Android only: exception here
			mod.setAccessible(true);
		} catch (Throwable e) {
			// It won't happen unless on android
		}
		f_oid = o;
		f_cid = c;
		f_eid = ev;
		f_modifiers = mod;
	}

	public NetworkWorker(CoreComponent owner) {
		this.owner = owner;
		this.w_buffer = new NetworkBuffer(owner.getGame().getOptions().getNetworkBufferSize());
		this.r_buffer = new NetworkBuffer(owner.getGame().getOptions().getNetworkBufferSize());
	}

	// Utility methods

	public void destroy() throws IOException {
		this.w_buffer = null;
		this.r_buffer = null;
		this.length_b = null;
		resetSocket();
	}

	public void provideSocket(Socket socket) {
		if (socket.isClosed() || socket == null) {
			throw new IllegalArgumentException("Invalid socket provided");
		}
		this.socket = socket;
	}

	public void provideAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void resetSocket() throws IOException {
		if (socket != null) {
			Socket s = this.socket;
			this.socket = null;
			if (!s.isClosed()) {
				s.close();
			}
		}
	}

	public void writeSocket() throws IOException {
		OutputStream os = socket.getOutputStream();
		int length = w_buffer.buffer.position();

		length_b[0] = (byte) ((length >> 24) & 0xff);
		length_b[1] = (byte) ((length >> 16) & 0xff);
		length_b[2] = (byte) ((length >> 8) & 0xff);
		length_b[3] = (byte) (length & 0xff);

		os.write(length_b);
		os.write(w_buffer.buffer.array(), 0, length);
		os.flush();

		w_buffer.buffer.clear();
	}

	public void readSocket() throws IOException {
		InputStream is = socket.getInputStream();
		is.read(length_b, 0, length_b.length);

		int length = length_b[3] & 0xFF | (length_b[2] & 0xFF) << 8 | (length_b[1] & 0xFF) << 16
				| (length_b[0] & 0xFF) << 24;

		int status = 0;
		int read = 0;
		while (read < length || read == -1) {
			status = is.read(r_buffer.buffer.array(), read, length - read);
			read += status;
		}

		r_buffer.buffer.clear();
	}

	// Reflection caching

	protected static Class<?> cls(String name) throws ClassNotFoundException {
		Class<?> cls = m_clss.get(name);
		if (cls == null) {
			synchronized (m_clss) {
				try {
					cls = Class.forName(name);
				} catch (ClassNotFoundException e) {
					Logger.error("Class not found: " + name);
					throw e;
				}
				m_clss.put(name, cls);
			}
		}
		return cls;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<?> o_constr(Class<?> cls) throws NoSuchMethodException, SecurityException {
		Constructor<?> constructor = m_oconstrs.get(cls);
		if (constructor == null) {
			synchronized (m_oconstrs) {
				constructor = cls.getConstructor(Level.class, GameObject.class);
				constructor.setAccessible(true);
				m_oconstrs.put(cls, (Constructor<? extends GameObject>) constructor);
			}
		}
		return constructor;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<?> c_constr(Class<?> cls) throws NoSuchMethodException, SecurityException {
		Constructor<?> constructor = m_cconstrs.get(cls);
		if (constructor == null) {
			synchronized (m_cconstrs) {
				constructor = cls.getConstructor();
				constructor.setAccessible(true);
				m_cconstrs.put(cls, (Constructor<? extends GameComponent>) constructor);
			}
		}
		return constructor;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<?> e_constr(Class<?> cls) throws NoSuchMethodException, SecurityException {
		Constructor<?> constructor = m_econstrs.get(cls);
		if (constructor == null) {
			synchronized (m_econstrs) {
				constructor = cls.getConstructor();
				constructor.setAccessible(true);
				m_econstrs.put(cls, (Constructor<? extends GameEvent>) constructor);
			}
		}
		return constructor;
	}

	protected static boolean noReplicate(Class<?> cls) {
		Boolean value = m_noreplicate.get(cls);
		if (value == null) {
			synchronized (m_noreplicate) {
				value = cls.getAnnotation(NoReplicate.class) != null;
				m_noreplicate.put(cls, value);
			}
		}
		return value;
	}

	protected static Field[] fields(Class<?> cls) {
		Field[] fields_ = m_fields.get(cls);
		if (fields_ == null) {
			synchronized (m_fields) {
				fields_ = gatherReplicable(cls);
				m_fields.put(cls, fields_);
			}
		}
		return fields_;
	}

	protected static Field[] gatherReplicable(Class<?> cls) {
		ArrayList<Field> list = null;
		try {
			// This method assumes cls extends GameObject, GameComponent or GameEvent

			list = new ArrayList<>();

			// Gather all declared m_fields from super classes until GameObject or
			// GameComponent is reached
			Class<?> current = cls;
			do {
				for (Field field : current.getDeclaredFields()) {
					if (field.getAnnotation(Replicate.class) != null) {
						list.add(field);
					}
				}
				current = current.getSuperclass();
			} while (current != null);

			// Remove any restriction from gathered m_fields
			for (Field field : list) {
				field.setAccessible(true);

				f_modifiers.set(field, field.getModifiers() & ~Modifier.FINAL);
			}

		} catch (Throwable t) {
			// Catch all exceptions for simplicity
			t.printStackTrace();
		}

		// Array must always be in the same order to work
		// regardless of the platform
		list.sort((field1, field2) -> field1.getName().compareTo(field2.getName()));
		list.sort((field1, field2) -> field1.getDeclaringClass().getName()
				.compareTo(field2.getDeclaringClass().getName()));

		// Cast to Field[]
		Field[] result = new Field[list.size()];
		for (int i = 0; i < list.size(); i++) {
			result[i] = list.get(i);
		}
		return result;
	}

	protected void writeField(Field field, Object obj) {
		try {

			Class<?> type = field.getType();

			// Primitives
			if (type.equals(byte.class) || type.equals(Byte.class)) {
				w_buffer.putByte((byte) field.get(obj));
			} else if (type.equals(short.class) || type.equals(Short.class)) {
				w_buffer.putShort((short) field.get(obj));
			} else if (type.equals(int.class) || type.equals(Integer.class)) {
				w_buffer.putInt((int) field.get(obj));
			} else if (type.equals(float.class) || type.equals(Float.class)) {
				w_buffer.putFloat((float) field.get(obj));
			} else if (type.equals(long.class) || type.equals(Long.class)) {
				w_buffer.putLong((long) field.get(obj));
			} else if (type.equals(double.class) || type.equals(Double.class)) {
				w_buffer.putDouble((double) field.get(obj));
			} else if (type.equals(char.class) || type.equals(Character.class)) {
				w_buffer.putChar((char) field.get(obj));
			} else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
				w_buffer.putBoolean((boolean) field.get(obj));
			}

			// Assets
			if (Asset.class.isAssignableFrom(type)) {
				Asset asset = (Asset) field.get(obj);
				w_buffer.putString(asset.getName());
			}
			
			// JOML Vectors
			if (type.equals(Vector2d.class)) {
				Vector2d vec = (Vector2d) field.get(obj);
				w_buffer.putDouble(vec.x);
				w_buffer.putDouble(vec.y);
			} else if (type.equals(Vector2f.class)) {
				Vector2f vec = (Vector2f) field.get(obj);
				w_buffer.putFloat(vec.x);
				w_buffer.putFloat(vec.y);
			} else if (type.equals(Vector2i.class)) {
				Vector2i vec = (Vector2i) field.get(obj);
				w_buffer.putInt(vec.x);
				w_buffer.putInt(vec.y);
			} else if (type.equals(Vector3d.class)) {
				Vector3d vec = (Vector3d) field.get(obj);
				w_buffer.putDouble(vec.x);
				w_buffer.putDouble(vec.y);
				w_buffer.putDouble(vec.z);
			} else if (type.equals(Vector3f.class)) {
				Vector3f vec = (Vector3f) field.get(obj);
				w_buffer.putFloat(vec.x);
				w_buffer.putFloat(vec.y);
				w_buffer.putFloat(vec.z);
			} else if (type.equals(Vector3i.class)) {
				Vector3i vec = (Vector3i) field.get(obj);
				w_buffer.putInt(vec.x);
				w_buffer.putInt(vec.y);
				w_buffer.putInt(vec.z);
			} else if (type.equals(Vector4d.class)) {
				Vector4d vec = (Vector4d) field.get(obj);
				w_buffer.putDouble(vec.x);
				w_buffer.putDouble(vec.y);
				w_buffer.putDouble(vec.z);
				w_buffer.putDouble(vec.w);
			} else if (type.equals(Vector4f.class)) {
				Vector4f vec = (Vector4f) field.get(obj);
				w_buffer.putFloat(vec.x);
				w_buffer.putFloat(vec.y);
				w_buffer.putFloat(vec.z);
				w_buffer.putFloat(vec.w);
			} else if (type.equals(Vector4i.class)) {
				Vector4i vec = (Vector4i) field.get(obj);
				w_buffer.putInt(vec.x);
				w_buffer.putInt(vec.y);
				w_buffer.putInt(vec.z);
				w_buffer.putInt(vec.w);
			}

		} catch (IllegalAccessException e) {
			// Already took care of that, won't happen
			e.printStackTrace();
		}
	}

	protected void readField(Field field, Object obj) {
		try {

			Class<?> type = field.getType();

			// Primitives
			if (type.equals(byte.class) || type.equals(Byte.class)) {
				field.set(obj, r_buffer.getByte());
			} else if (type.equals(short.class) || type.equals(Short.class)) {
				field.set(obj, r_buffer.getShort());
			} else if (type.equals(int.class) || type.equals(Integer.class)) {
				field.set(obj, r_buffer.getInt());
			} else if (type.equals(float.class) || type.equals(Float.class)) {
				field.set(obj, r_buffer.getFloat());
			} else if (type.equals(long.class) || type.equals(Long.class)) {
				field.set(obj, r_buffer.getLong());
			} else if (type.equals(double.class) || type.equals(Double.class)) {
				field.set(obj, r_buffer.getDouble());
			} else if (type.equals(char.class) || type.equals(Character.class)) {
				field.set(obj, r_buffer.getChar());
			} else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
				field.set(obj, r_buffer.getBoolean());
			}

			// Assets
			if(Asset.class.isAssignableFrom(type)) {
				AssetManager asset_manager = owner.getGame().getAssetManager();
				String asset_name = r_buffer.getString();
				Asset asset = asset_manager.custom(asset_name);
				field.set(obj, asset);
			}
			
			// JOML Vectors
			if (type.equals(Vector2d.class)) {
				Vector2d vec = (Vector2d) field.get(obj);
				vec.x = r_buffer.getDouble();
				vec.y = r_buffer.getDouble();
			} else if (type.equals(Vector2f.class)) {
				Vector2f vec = (Vector2f) field.get(obj);
				vec.x = r_buffer.getFloat();
				vec.y = r_buffer.getFloat();
			} else if (type.equals(Vector2i.class)) {
				Vector2i vec = (Vector2i) field.get(obj);
				vec.x = r_buffer.getInt();
				vec.y = r_buffer.getInt();
			} else if (type.equals(Vector3d.class)) {
				Vector3d vec = (Vector3d) field.get(obj);
				vec.x = r_buffer.getDouble();
				vec.y = r_buffer.getDouble();
				vec.z = r_buffer.getDouble();
			} else if (type.equals(Vector3f.class)) {
				Vector3f vec = (Vector3f) field.get(obj);
				vec.x = r_buffer.getFloat();
				vec.y = r_buffer.getFloat();
				vec.z = r_buffer.getFloat();
			} else if (type.equals(Vector3i.class)) {
				Vector3i vec = (Vector3i) field.get(obj);
				vec.x = r_buffer.getInt();
				vec.y = r_buffer.getInt();
				vec.z = r_buffer.getInt();
			} else if (type.equals(Vector4d.class)) {
				Vector4d vec = (Vector4d) field.get(obj);
				vec.x = r_buffer.getDouble();
				vec.y = r_buffer.getDouble();
				vec.z = r_buffer.getDouble();
				vec.w = r_buffer.getDouble();
			} else if (type.equals(Vector4f.class)) {
				Vector4f vec = (Vector4f) field.get(obj);
				vec.x = r_buffer.getFloat();
				vec.y = r_buffer.getFloat();
				vec.z = r_buffer.getFloat();
				vec.w = r_buffer.getFloat();
			} else if (type.equals(Vector4i.class)) {
				Vector4i vec = (Vector4i) field.get(obj);
				vec.x = r_buffer.getInt();
				vec.y = r_buffer.getInt();
				vec.z = r_buffer.getInt();
				vec.w = r_buffer.getInt();
			}

		} catch (IllegalAccessException e) {
			// Already took care of that, won't happen
			e.printStackTrace();
		}
	}

	// Write functions

	public void writeLevel() {
		Level level = getLevel();

		// Write metadata
		w_buffer.putByte(DataType.LEVEL);
		level.forEachObject(this::writeChildren);
		w_buffer.putByte(DataType.STOP);
		
		w_buffer.putLong(level.getActiveCamera() == null ? -1 : level.getActiveCamera().getId());
	}

	public void writeChildren(GameObject obj) {
		if (noReplicate(obj.getClass())) {
			return;
		}
		w_buffer.putByte(DataType.ITEM);

		// Write info about this object
		w_buffer.putLong(obj.getId());
		w_buffer.putString(obj.getClass().getName());
		// Let the object write custom data
		writeObjectCustom(obj);

		// Write info about its components
		obj.forEachComponent((id, component) -> {
			if (!noReplicate(component.getClass())) {
				w_buffer.putByte(DataType.ITEM);
				w_buffer.putLong(component.getId());
				w_buffer.putString(component.getClass().getName());
				// Let the component write custom data
				writeComponentCustom(component);
			}
		});
		w_buffer.putByte(DataType.STOP);

		// Recursively write info about its children
		obj.forEachChild((id, child) -> writeChildren(child));
		w_buffer.putByte(DataType.STOP);
	}

	public void writeObject(GameObject obj) {
		w_buffer.putByte(DataType.GAMEOBJECT);
		w_buffer.putLong(obj.getId());
		// Let the object write custom data
		writeObjectCustom(obj);
	}

	public void writeComponent(GameComponent comp) {
		w_buffer.putByte(DataType.GAMECOMPONENT);
		w_buffer.putLong(comp.getId());
		// Let the component write custom data
		writeComponentCustom(comp);
	}

	protected void writeObjectCustom(GameObject obj) {
		// Here we write fields flagged with @Replicate in GameObjects
		for (Field field : fields(obj.getClass())) {
			writeField(field, obj);
		}
		obj.writeData(w_buffer);
	}

	protected void writeComponentCustom(GameComponent comp) {
		// Here we write fields flagged with @Replicate in GameComponents
		for (Field field : fields(comp.getClass())) {
			writeField(field, comp);
		}
		comp.writeData(w_buffer);
	}

	protected void writeEventCustom(GameEvent event) {
		// Here we write fields flagged with @Replicate in GameEvents
		for (Field field : fields(event.getClass())) {
			writeField(field, event);
		}
	}

	public void writeIntention(GameObject issuer, long intention) {
		w_buffer.putByte(DataType.INTENTION);
		w_buffer.putLong(issuer.getId());
		w_buffer.putLong(intention);
	}

	public void writeGameEvent(GameObject issuer, GameEvent event) {
		w_buffer.putByte(DataType.GAMEEVENT);
		w_buffer.putLong(issuer.getId());

		w_buffer.putString(event.getClass().getName());
		w_buffer.putLong(event.getId());
		writeEventCustom(event);
	}

	public void writeAuthentication() {
		if (authenticator == null) {
			authenticator = new DefaultAuthenticator();
		}
		if (owner.getGame().isClient()) {
			authenticator.w_client_auth(this, w_buffer);
		} else {
			authenticator.w_server_auth(this, w_buffer);
		}
	}

	// Read functions

	protected void invalid(byte opcode) {
		Logger.warning("Remote socket tried to perform an invalid operation (" + (opcode & 0xff)
				+ ") and the connection must be terminated");
		try {
			resetSocket();
		} catch (IOException e) {
			Logger.error("I/O error occurred while resetting connection, ignoring");
		}
	}

	public void readData() throws Throwable {
		byte opcode = r_buffer.getByte();
		Level level = getLevel();
		switch (opcode) {
		default:
			invalid(opcode);
			return;
		case DataType.LEVEL:
			readLevel();
			break;
		case DataType.GAMEOBJECT:
			readObject(level);
			break;
		case DataType.GAMECOMPONENT:
			readComponent(level);
			break;
		case DataType.GAMEEVENT:
			readGameEvent();
			break;
		case DataType.INTENTION:
			readIntention();
			break;
		}
	}

	public void readLevel() throws Throwable {
		Level level = getLevel();

		// Read all children
		while (r_buffer.getByte() == DataType.ITEM) {
			readChildren(level, (GameObject) null);
		}
		
		long activeCamera = r_buffer.getLong();
		if(activeCamera != -1) {
			Camera camera = (Camera) level.getObject(activeCamera);
			if(level.getActiveCamera() != camera) {
				if(level.getActiveCamera() != null) {
					level.detachCamera();
				}
				level.attachCamera(camera);
			}
		}
	}

	public void readChildren(Level level, GameObject parent) throws ClassCastException, InstantiationException,
			InvocationTargetException, NoSuchMethodException, ClassNotFoundException {

		try {
			// Parse data from this object
			long id = r_buffer.getLong();
			String clazz = r_buffer.getString();

			GameObject object = null;
			if ((object = level.getObject(id)) == null) {
				// Build the object if not present
				object = (GameObject) o_constr(cls(clazz)).newInstance(level, parent);
			}
			f_oid.set(object, id);
			readObjectCustom(object);

			// Parse its components
			while (r_buffer.getByte() == DataType.ITEM) {
				long comp_id = r_buffer.getLong();
				String comp_clazz = r_buffer.getString();

				GameComponent component = null;
				if ((component = object.getComponent(id)) == null) {
					// Build and add the component if not present
					component = (GameComponent) c_constr(cls(comp_clazz)).newInstance();
					object.addComponent(component);
				}
				f_cid.set(component, comp_id);
				readComponentCustom(component);
			}

			// Recursively read data from its children
			while (r_buffer.getByte() == DataType.ITEM) {
				readChildren(level, object);
			}
		} catch (IllegalAccessException e) {
			// This should not happen even with errors in the parsing
			e.printStackTrace();
		}

	}

	public void readObject(Level level) {
		long id = r_buffer.getLong();
		GameObject obj = level.getObject(id);
		// Read back custom data from object
		readObjectCustom(obj);
	}

	public void readComponent(Level level) {
		long id = r_buffer.getLong();
		GameComponent comp = level.getComponent(id);
		// Read back custom data from component
		readComponentCustom(comp);
	}

	protected void readObjectCustom(GameObject obj) {
		// Here we read m_fields flagged with @Replicate in GameObjects
		for (Field field : fields(obj.getClass())) {
			readField(field, obj);
		}
		obj.readData(r_buffer);
	}

	protected void readComponentCustom(GameComponent comp) {
		// Here we read m_fields flagged with @Replicate in GameComponents
		for (Field field : fields(comp.getClass())) {
			readField(field, comp);
		}
		comp.readData(r_buffer);
	}

	protected void readEventCustom(GameEvent event) {
		// Here we read fields flagged with @Replicate in GameEvents
		for (Field field : fields(event.getClass())) {
			readField(field, event);
		}
	}

	public void readIntention() {
		long issuer_id = r_buffer.getLong();
		long intention = r_buffer.getLong();

		Level level = getLevel();
		GameObject issuer = level.getObject(issuer_id);
		EventDispatcher event_dispatcher = owner.getGame().getEventDispatcher();

		event_dispatcher.dispatchIntention(IDENTITY, issuer, intention);
	}

	public void readGameEvent() throws ClassCastException, InstantiationException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException {

		try {

			long issuer_id = r_buffer.getLong();
			String event_class = r_buffer.getString();
			long event_id = r_buffer.getLong();

			Level level = getLevel();
			GameObject issuer = level.getObject(issuer_id);
			GameEvent event = (GameEvent) e_constr(cls(event_class)).newInstance();
			f_eid.set(event, event_id);
			EventDispatcher event_dispatcher = owner.getGame().getEventDispatcher();
			readEventCustom(event);

			event_dispatcher.dispatchEvent(IDENTITY, issuer, event);

		} catch (IllegalAccessException e) {
			// This should not happen even with errors in the parsing
			e.printStackTrace();
		}

	}

	public void readAuthentication() {
		if (authenticator == null) {
			authenticator = new DefaultAuthenticator();
		}
		if (owner.getGame().isClient()) {
			authenticator.r_client_auth(this, r_buffer);
		} else {
			authenticator.r_server_auth(this, r_buffer);
		}
	}

	// Getters and setters

	public Game getGame() {
		return owner.getGame();
	}

	public Level getLevel() {
		if (getGame().getActiveLevel() == null) {
			getGame().attachLevel(new Level());
		}
		return getGame().getActiveLevel();
	}

	public Socket getSocket() {
		return socket;
	}

	public boolean isConnected() {
		return socket != null && !socket.isClosed();
	}

	public NetworkBuffer getWriteBuffer() {
		return w_buffer;
	}

	public NetworkBuffer getReadBuffer() {
		return r_buffer;
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public String getRemoteIp() {
		InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
		return address.getAddress().getHostAddress();
	}

	public int getRemotePort() {
		InetSocketAddress address = (InetSocketAddress) socket.getRemoteSocketAddress();
		return address.getPort();
	}

	public String getLocalIp() {
		InetSocketAddress address = (InetSocketAddress) socket.getLocalSocketAddress();
		return address.getAddress().getHostAddress();
	}

	public int getLocalPort() {
		InetSocketAddress address = (InetSocketAddress) socket.getLocalSocketAddress();
		return address.getPort();
	}

}