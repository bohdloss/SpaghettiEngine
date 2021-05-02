package com.spaghetti.networking;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.spaghetti.core.*;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.GameEvent;
import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.*;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.GameOptions;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public class NetworkWorker {

	public static final class Identity {
		private Identity() {
		}
	}

	protected static final Identity IDENTITY = new Identity();
	protected static final byte[] PING = "ping".getBytes(NetworkBuffer.UTF_8);
	protected static final byte[] PONG = "pong".getBytes(NetworkBuffer.UTF_8);

	// Data
	protected long lostConnection;
	protected CoreComponent parent;
	protected NetworkBuffer w_buffer;
	protected NetworkBuffer r_buffer;
	protected Socket socket;
	protected Authenticator authenticator;
	protected boolean ping = true;
	protected boolean await;
	protected HashMap<Class<?>, ClassReplicationRule> cls_rules;
	protected HashMap<Field, FieldReplicationRule> field_rules;
	protected boolean forceReplication;

	// Cache
	protected final byte[] ping_buf = new byte[PING.length];
	protected final byte[] pong_buf = new byte[PONG.length];
	protected final ArrayList<Object> delete_list = new ArrayList<>(256);
	protected final HashMap<Short, String> str_cache = new HashMap<>(256);

	// Player info
	public GameObject player;
	public GameComponent player_controller;
	public Camera player_camera;

	protected byte[] length_b = new byte[4];

	// Reflection
	protected static final Field f_oid, f_cid, f_eid;
	protected static final Field f_modifiers;
	protected static final Method me_osetflag, me_csetflag, me_ogetflag, me_cgetflag;

	protected static final HashMap<String, Class<?>> m_clss = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameObject>> m_oconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameComponent>> m_cconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameEvent>> m_econstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Field[]> m_fields = new HashMap<>();
	protected static final HashMap<Class<?>, Boolean> m_noreplicate = new HashMap<>();
	protected static final HashMap<Field, Boolean> m_toserver = new HashMap<>();
	protected static final HashMap<Class<?>, Boolean> m_toservercls = new HashMap<>();

	// If only java let me define 'friend class NetworkWorker' in GameObject...
	static {
		Field oid = null;
		Field cid = null;
		Field eid = null;

		Method osetflag = null;
		Method csetflag = null;
		Method ogetflag = null;
		Method cgetflag = null;

		Field modifiers = null;

		try {
			oid = GameObject.class.getDeclaredField("id");
			oid.setAccessible(true);
			cid = GameComponent.class.getDeclaredField("id");
			cid.setAccessible(true);
			eid = GameEvent.class.getDeclaredField("id");
			eid.setAccessible(true);

			osetflag = GameObject.class.getDeclaredMethod("internal_setflag", int.class, boolean.class);
			osetflag.setAccessible(true);
			csetflag = GameComponent.class.getDeclaredMethod("internal_setflag", int.class, boolean.class);
			csetflag.setAccessible(true);
			ogetflag = GameObject.class.getDeclaredMethod("internal_getflag", int.class);
			ogetflag.setAccessible(true);
			cgetflag = GameComponent.class.getDeclaredMethod("internal_getflag", int.class);
			cgetflag.setAccessible(true);

			modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
		} catch (Throwable e) {
		}

		f_oid = oid;
		f_cid = cid;
		f_eid = eid;

		me_osetflag = osetflag;
		me_csetflag = csetflag;
		me_ogetflag = ogetflag;
		me_cgetflag = cgetflag;

		f_modifiers = modifiers;
	}

	protected final HashMap<String, ClassInterpreter> interpreters = new HashMap<>();

	public NetworkWorker(CoreComponent parent) {
		this.parent = parent;
		this.w_buffer = new NetworkBuffer(this,
				Game.getGame().getOptions().getOption(GameOptions.PREFIX + "networkbuffer"));
		this.r_buffer = new NetworkBuffer(this,
				Game.getGame().getOptions().getOption(GameOptions.PREFIX + "networkbuffer"));

		cls_rules = ClassReplicationRule.rules.get(parent.getGame());
		if (cls_rules == null) {
			HashMap<Class<?>, ClassReplicationRule> hm = new HashMap<>();
			ClassReplicationRule.rules.put(parent.getGame(), hm);
			cls_rules = hm;
		}

		field_rules = FieldReplicationRule.rules.get(parent.getGame());
		if (field_rules == null) {
			HashMap<Field, FieldReplicationRule> hm = new HashMap<>();
			FieldReplicationRule.rules.put(parent.getGame(), hm);
			field_rules = hm;
		}

		registerInterpreters();
	}

	protected void registerInterpreters() {
		interpreters.putAll(DefaultInterpreters.interpreters);
	}

	// Utility methods

	public void destroy() {
		try {
			if (player != null) {
				player.destroy();
			}
		} catch (Throwable t) {
			Logger.error("Error deleting player", t);
		}
		try {
			if (player_camera != null) {
				player_camera.destroy();
			}
		} catch (Throwable t) {
			Logger.error("Error deleting player camera", t);
		}
		try {
			if (player_controller != null) {
				player_controller.destroy();
			}
		} catch (Throwable t) {
			Logger.error("Error deleting player controller", t);
		}
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
		ping = true;
		str_cache.clear();
	}

	public void provideAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	public void resetSocket() {
		if (socket != null) {
			Socket s = this.socket;
			this.socket = null;
			Utils.socketClose(s);
			str_cache.clear();
		}
	}

	public void writeSocket() throws IOException {
		w_buffer.putByte(Opcode.END);

		OutputStream os = socket.getOutputStream();
		int length = w_buffer.getPosition();

		length_b[0] = (byte) ((length >> 24) & 0xff);
		length_b[1] = (byte) ((length >> 16) & 0xff);
		length_b[2] = (byte) ((length >> 8) & 0xff);
		length_b[3] = (byte) (length & 0xff);

		os.write(length_b);
		os.write(w_buffer.asArray(), 0, length);
		os.flush();

		w_buffer.clear();
	}

	public void readSocket() throws IOException {
		InputStream is = socket.getInputStream();
		Utils.effectiveRead(is, length_b, 0, 4);
		int length = length_b[3] & 0xFF | (length_b[2] & 0xFF) << 8 | (length_b[1] & 0xFF) << 16
				| (length_b[0] & 0xFF) << 24;

		Utils.effectiveRead(is, r_buffer.asArray(), 0, length);
		r_buffer.setPosition(0);
		r_buffer.setLimit(length);
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
	protected static Constructor<?> o_constr(Class<?> cls) throws NoSuchMethodException {
		Constructor<?> constructor = m_oconstrs.get(cls);
		if (constructor == null) {
			synchronized (m_oconstrs) {
				try {
					constructor = cls.getConstructor();
					constructor.setAccessible(true);
					m_oconstrs.put(cls, (Constructor<? extends GameObject>) constructor);
				} catch (NoSuchMethodException e) {
					Logger.error("Class " + cls.getName() + " must provide a default GameObject constructor");
					throw e;
				}
			}
		}
		return constructor;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<?> c_constr(Class<?> cls) throws NoSuchMethodException {
		Constructor<?> constructor = m_cconstrs.get(cls);
		if (constructor == null) {
			synchronized (m_cconstrs) {
				try {
					constructor = cls.getConstructor();
					constructor.setAccessible(true);
					m_cconstrs.put(cls, (Constructor<? extends GameComponent>) constructor);
				} catch (NoSuchMethodException e) {
					Logger.error("Class " + cls.getName() + " must provide a default GameComponent constructor");
					throw e;
				}
			}
		}
		return constructor;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<?> e_constr(Class<?> cls) throws NoSuchMethodException {
		Constructor<?> constructor = m_econstrs.get(cls);
		if (constructor == null) {
			synchronized (m_econstrs) {
				try {
					constructor = cls.getConstructor();
					constructor.setAccessible(true);
					m_econstrs.put(cls, (Constructor<? extends GameEvent>) constructor);
				} catch (NoSuchMethodException e) {
					Logger.error("Class " + cls.getName() + " must provide a default GameEvent constructor");
					throw e;
				}
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

	protected boolean test_writeClass(Object obj) {
		ClassReplicationRule rule = cls_rules.get(obj.getClass());
		if (rule == null) {
			rule = new ClassReplicationRule(obj.getClass());
		}
		return rule.testWrite();
	}

	protected boolean test_readClass(Object obj) {
		ClassReplicationRule rule = cls_rules.get(obj.getClass());
		if (rule == null) {
			rule = new ClassReplicationRule(obj.getClass());
		}
		return rule.testRead();
	}

	protected boolean test_needReplication(boolean gameobject, Object obj) {
		boolean val;
		if (gameobject) {
			val = ((GameObject) obj).getReplicateFlag();
		} else {
			val = ((GameComponent) obj).getReplicateFlag();
		}
		return val || forceReplication;
	}

	protected boolean test_writeField(Field field) {
		FieldReplicationRule rule = field_rules.get(field);
		if (rule == null) {
			rule = new FieldReplicationRule(field);
		}
		return rule.testWrite();
	}

	protected boolean test_readField(Field field) {
		FieldReplicationRule rule = field_rules.get(field);
		if (rule == null) {
			rule = new FieldReplicationRule(field);
		}
		return rule.testRead();
	}

	protected static Field[] gatherReplicable(Class<?> cls) {
		ArrayList<Field> list = null;
		try {
			// This method assumes cls extends GameObject, GameComponent or GameEvent

			list = new ArrayList<>();

			// Gather all declared fields from super classes
			Class<?> current = cls;
			do {
				for (Field field : current.getDeclaredFields()) {
					if (field.getAnnotation(Replicate.class) != null) {
						list.add(field);
					}
				}
				current = current.getSuperclass();
			} while (current != null);

			// Remove any restriction from gathered fields
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
			ClassInterpreter interpreter = interpreters.get(type.getName());
			if (interpreter != null) {
				interpreter.writeClass(field, obj, w_buffer);
			} else {
				for (Entry<String, ClassInterpreter> entry : interpreters.entrySet()) {
					try {
						Class<?> cls = Class.forName(entry.getKey());
						if (cls.isAssignableFrom(type)) {
							entry.getValue().writeClass(field, obj, w_buffer);
							break;
						}
					} catch (ClassNotFoundException e) {
					}
				}
			}
		} catch (IllegalAccessException e) {
		}
	}

	protected void readField(Field field, Object obj) {
		try {
			Class<?> type = field.getType();
			ClassInterpreter interpreter = interpreters.get(type.getName());
			if (interpreter != null) {
				interpreter.readClass(field, obj, r_buffer);
			} else {
				for (Entry<String, ClassInterpreter> entry : interpreters.entrySet()) {
					try {
						Class<?> cls = Class.forName(entry.getKey());
						if (cls.isAssignableFrom(type)) {
							entry.getValue().readClass(field, obj, r_buffer);
							break;
						}
					} catch (ClassNotFoundException e) {
					}
				}
			}
		} catch (IllegalAccessException e) {
		}
	}

	protected void writeObjectCustom(GameObject obj) {
		// Here we write fields flagged with @Replicate in GameObjects
		for (Field field : fields(obj.getClass())) {
			if (test_writeField(field)) {
				writeField(field, obj);
			}
		}
		if (getGame().isClient()) {
			obj.writeDataClient(w_buffer);
		} else {
			obj.writeDataServer(w_buffer);
		}
	}

	protected void readObjectCustom(GameObject obj) {
		// Here we read fields flagged with @Replicate in GameObjects
		for (Field field : fields(obj.getClass())) {
			if (test_readField(field)) {
				readField(field, obj);
			}
		}
		if (getGame().isClient()) {
			obj.readDataClient(r_buffer);
		} else {
			obj.readDataServer(r_buffer);
		}
	}

	protected void writeComponentCustom(GameComponent comp) {
		// Here we write fields flagged with @Replicate in GameComponents
		for (Field field : fields(comp.getClass())) {
			if (test_writeField(field)) {
				writeField(field, comp);
			}
		}
		if (getGame().isClient()) {
			comp.writeDataClient(w_buffer);
		} else {
			comp.writeDataServer(w_buffer);
		}
	}

	protected void readComponentCustom(GameComponent comp) {
		// Here we read fields flagged with @Replicate in GameComponents
		for (Field field : fields(comp.getClass())) {
			if (test_readField(field)) {
				readField(field, comp);
			}
		}
		if (getGame().isClient()) {
			comp.writeDataClient(w_buffer);
		} else {
			comp.writeDataServer(w_buffer);
		}
	}

	protected void writeEventCustom(GameEvent event) {
		// Here we write fields flagged with @Replicate in GameEvents
		for (Field field : fields(event.getClass())) {
			writeField(field, event);
		}
	}

	protected void readEventCustom(GameEvent event) {
		// Here we read fields flagged with @Replicate in GameEvents
		for (Field field : fields(event.getClass())) {
			readField(field, event);
		}
	}

	// Read / Write interface

	public void parseOperations() throws Throwable {
		byte opcode;
		Level level = getLevel();
		while ((opcode = r_buffer.getByte()) != Opcode.END) {
			switch (opcode) {
			default:
				invalid(opcode);
				return;
			case Opcode.LEVEL:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readLevel();
				break;
			case Opcode.GAMEOBJECT:
				readObject(level);
				break;
			case Opcode.GAMECOMPONENT:
				readComponent(level);
				break;
			case Opcode.GAMEEVENT:
				readGameEvent();
				break;
			case Opcode.INTENTION:
				readIntention();
				break;
			case Opcode.CAMERA:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readActiveCamera(level);
				break;
			case Opcode.CONTROLLER:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readActiveController(level);
				break;
			case Opcode.PLAYER:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readActivePlayer(level);
				break;
			case Opcode.DATA:
				readData();
				break;
			case Opcode.PINGPONG:
				handlePing();
				break;
			}
		}
	}

	public void writeData() {
		Level level = getLevel();
		w_buffer.putByte(Opcode.DATA);

		// Write objects
		level.forEachActualObject((id, object) -> {
			if (test_writeClass(object) && test_needReplication(true, object)) {
				w_buffer.putByte(Opcode.ITEM);
				w_buffer.putInt(object.getId());
				int pos = w_buffer.getPosition();
				w_buffer.skip(Short.BYTES); // Allocate memory for skip destination
				writeObjectCustom(object);
				int off = w_buffer.getPosition() - (pos + Short.BYTES);
				w_buffer.putShortAt(pos, (short) off); // Write destination
			}
		});
		w_buffer.putByte(Opcode.STOP);

		// Write components
		level.forEachComponent((id, component) -> {
			if (test_writeClass(component) && test_needReplication(false, component)) {
				w_buffer.putByte(Opcode.ITEM);
				w_buffer.putInt(component.getId());
				int pos = w_buffer.getPosition();
				w_buffer.skip(Short.BYTES); // Allocate memory for skip destination
				writeComponentCustom(component);
				int off = w_buffer.getPosition() - (pos + Short.BYTES);
				w_buffer.putShortAt(pos, (short) off); // Write destination
			}
		});
		w_buffer.putByte(Opcode.STOP);
	}

	public void readData() {
		Level level = getLevel();

		// Read objects
		while (r_buffer.getByte() == Opcode.ITEM) {
			int id = r_buffer.getInt();
			short skip = r_buffer.getShort();
			if (skip < 0) {
				throw new IllegalStateException("Negative skip value");
			}
			GameObject object = level.getObject(id);
			if (object == null || !test_readClass(object)) {
				r_buffer.skip(skip);
				continue;
			}
			readObjectCustom(object);
		}

		// Read components
		while (r_buffer.getByte() == Opcode.ITEM) {
			int id = r_buffer.getInt();
			short skip = r_buffer.getShort();
			if (skip < 0) {
				throw new IllegalStateException("Negative skip value");
			}
			GameComponent component = level.getComponent(id);
			if (component == null || !test_readClass(component)) {
				r_buffer.skip(skip);
				continue;
			}
			readComponentCustom(component);
		}
	}

	public void writeLevel() {
		Level level = getLevel();

		// Write metadata
		w_buffer.putByte(Opcode.LEVEL);
		synchronized (level) {
			level.forEachObject(this::writeChildren);
		}
		w_buffer.putByte(Opcode.STOP);
	}

	public void readLevel() throws Throwable {
		Level level = getLevel();

		// First flag all objects as deletable
		// this will be reverted by readChildren
		// but only on objects that actually exist
		level.forEachObject(this::recursive_flag);

		// Check for item flag
		while (r_buffer.getByte() == Opcode.ITEM) {
			readChildren(level, (GameObject) null);
		}

		// If the objects are still flagged, it means
		// they no longer exist, therefore they will
		// be deleted
		level.forEachObject(this::recursive_delete);
		perform_delete();
	}

	public void writeActiveCamera() {
		w_buffer.putByte(Opcode.CAMERA);
		w_buffer.putInt(player_camera == null ? -1 : player_camera.getId());
	}

	public void readActiveCamera(Level level) {
		int camera_id = r_buffer.getInt();
		// -1 means null
		if (camera_id != -1) {
			// Obtain level camera
			Camera level_camera = (Camera) level.getObject(camera_id);

			// Check differences with local camera
			if (level_camera != player_camera) {
				// Apply changes
				player_camera = level_camera;
				level.attachCamera(level_camera);
			}
		} else {
			player_camera = null;
			level.detachCamera();
		}
	}

	public void writeActiveController() {
		w_buffer.putByte(Opcode.CONTROLLER);
		w_buffer.putInt(player_controller == null ? -1 : player_controller.getId());
	}

	public void readActiveController(Level level) {
		int controller_id = r_buffer.getInt();
		// -1 means null
		if (controller_id != -1) {
			// Obtain level controller
			Controller level_controller = (Controller) level.getComponent(controller_id);

			// Check differences with local controller
			if (level_controller != player_controller) {
				// Apply changes
				player_controller = level_controller;
				level.attachController(level_controller);
			}
		} else {
			player_controller = null;
			level.detachController();
		}
	}

	public void writeActivePlayer() {
		w_buffer.putByte(Opcode.PLAYER);
		w_buffer.putInt(player == null ? -1 : player.getId());
	}

	public void readActivePlayer(Level level) {
		int player_id = r_buffer.getInt();
		// -1 means null
		if (player_id != -1) {
			// Obtain level player
			GameObject level_player = level.getObject(player_id);

			// Check differences with local player
			if (level_player != player) {
				// Apply changes
				player = level_player;
			}
		} else {
			player = null;
		}
	}

	public void writeChildren(GameObject obj) {
		if (noReplicate(obj.getClass())) {
			return;
		}
		w_buffer.putByte(Opcode.ITEM); // Put item flag

		w_buffer.putInt(obj.getId()); // Put id of the object
		w_buffer.putString(true, obj.getClass().getName(), NetworkBuffer.UTF_8); // Put class of the object

		obj.forEachComponent((id, component) -> {
			if (!noReplicate(component.getClass())) {
				w_buffer.putByte(Opcode.ITEM); // Put item flag

				w_buffer.putInt(component.getId()); // Put id of component
				w_buffer.putString(true, component.getClass().getName(), NetworkBuffer.UTF_8); // Put class of component
			}
		});
		w_buffer.putByte(Opcode.STOP); // Put stop flag

		obj.forEachChild((id, child) -> writeChildren(child));
		w_buffer.putByte(Opcode.STOP); // Put stop flag on children
	}

	public void readChildren(Level level, GameObject parent)
			throws InstantiationException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {

		try {
			int id = r_buffer.getInt(); // Get id of object
			String clazz = r_buffer.getString(true, NetworkBuffer.UTF_8); // Get class of object

			GameObject object = level.getObject(id);
			if (object == null) {
				// Build and add the object if not present
				Class<?> objclass = cls(clazz);
				if (!GameObject.class.isAssignableFrom(objclass)) {
					throw new IllegalStateException("Invalid object class");
				}
				object = (GameObject) o_constr(objclass).newInstance();
				f_oid.set(object, id);
				if (parent == null) {
					level.addObject(object);
				} else {
					parent.addChild(object);
				}
			}
			me_osetflag.invoke(object, GameObject.DELETE, false);

			// Check for item flag
			while (r_buffer.getByte() == Opcode.ITEM) {
				int comp_id = r_buffer.getInt(); // Get id of component
				String comp_clazz = r_buffer.getString(true, NetworkBuffer.UTF_8); // Get class of component

				GameComponent component = object.getComponent(comp_id);
				if (component == null) {
					// Build and add the component if not present
					Class<?> compclass = cls(comp_clazz);
					if (!GameComponent.class.isAssignableFrom(compclass)) {
						throw new IllegalStateException("Invalid component class");
					}
					component = (GameComponent) c_constr(compclass).newInstance();
					f_cid.set(component, comp_id);
					object.addComponent(component);
				}
				me_csetflag.invoke(component, GameComponent.DELETE, false);
			}

			// Check for item flag on children
			while (r_buffer.getByte() == Opcode.ITEM) {
				readChildren(level, object);
			}
		} catch (IllegalAccessException e) {
		}

	}

	public void writeObject(GameObject obj) {
		if (!test_writeClass(obj) || !test_needReplication(true, obj)) {
			return;
		}
		w_buffer.putByte(Opcode.GAMEOBJECT);
		int pos = w_buffer.getPosition();
		w_buffer.skip(Integer.BYTES);
		w_buffer.putInt(obj.getId());
		// Let the object write custom data
		writeObjectCustom(obj);
		int dest = w_buffer.getPosition();
		w_buffer.putIntAt(pos, dest);
	}

	public void readObject(Level level) {
		int skip = r_buffer.getInt();
		if (skip < r_buffer.getPosition()) {
			throw new IllegalStateException();
		}
		int id = r_buffer.getInt();
		GameObject obj = level.getObject(id);
		if (obj == null) {
			r_buffer.setPosition(skip);
			return;
		}
		if (!test_readClass(obj)) {
			if (getGame().isServer()) {
				invalid_privilege(Opcode.GAMEOBJECT);
			}
			r_buffer.setPosition(skip);
			return;
		}
		// Read back custom data from object
		readObjectCustom(obj);
	}

	public void writeComponent(GameComponent comp) {
		if (!test_writeClass(comp) || test_needReplication(false, comp)) {
			return;
		}
		w_buffer.putByte(Opcode.GAMECOMPONENT);
		int pos = w_buffer.getPosition();
		w_buffer.skip(Integer.BYTES);
		w_buffer.putInt(comp.getId());
		// Let the component write custom data
		writeComponentCustom(comp);
		int dest = w_buffer.getPosition();
		w_buffer.putIntAt(pos, dest);
	}

	public void readComponent(Level level) {
		int skip = r_buffer.getInt();
		if (skip < r_buffer.getPosition()) {
			throw new IllegalStateException();
		}
		int id = r_buffer.getInt();
		GameComponent comp = level.getComponent(id);
		if (comp == null) {
			r_buffer.setPosition(skip);
			return;
		}
		if (!test_readClass(comp)) {
			if (getGame().isServer()) {
				invalid_privilege(Opcode.GAMEOBJECT);
			}
			r_buffer.setPosition(skip);
			return;
		}
		// Read back custom data from component
		readComponentCustom(comp);
	}

	public void writeIntention(GameObject issuer, long intention) {
		w_buffer.putByte(Opcode.INTENTION);
		w_buffer.putInt(issuer == null ? -1 : issuer.getId());
		w_buffer.putLong(intention);
	}

	public void readIntention() {
		int issuer_id = r_buffer.getInt();
		long intention = r_buffer.getLong();

		Level level = getLevel();
		GameObject issuer = issuer_id == -1 ? null : level.getObject(issuer_id);
		FunctionDispatcher func_dispatcher = getGame().getUpdaterDispatcher();
		EventDispatcher event_dispatcher = getGame().getEventDispatcher();

		func_dispatcher.queue(() -> {
			event_dispatcher.dispatchIntention(IDENTITY, issuer, intention);
			return null;
		}, true);
	}

	public void writeGameEvent(GameObject issuer, GameEvent event) {
		w_buffer.putByte(Opcode.GAMEEVENT);
		w_buffer.putInt(issuer == null ? -1 : issuer.getId());

		w_buffer.putString(event.getClass().getName());
		w_buffer.putInt(event.getId());
		writeEventCustom(event);
	}

	public void readGameEvent() throws InstantiationException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException {

		try {

			int issuer_id = r_buffer.getInt();
			String event_class = r_buffer.getString();
			int event_id = r_buffer.getInt();

			Level level = getLevel();
			GameObject issuer = issuer_id == -1 ? null : level.getObject(issuer_id);
			Class<?> eventclass = cls(event_class);
			if (!GameEvent.class.isAssignableFrom(eventclass)) {
				throw new IllegalStateException("Invalid event class");
			}
			GameEvent event = (GameEvent) e_constr(eventclass).newInstance();
			f_eid.set(event, event_id);
			EventDispatcher event_dispatcher = getGame().getEventDispatcher();
			FunctionDispatcher func_dispatcher = getGame().getUpdaterDispatcher();
			readEventCustom(event);
			event.setFrom(getGame().isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

			func_dispatcher.queue(() -> {
				event_dispatcher.dispatchEvent(IDENTITY, issuer, event);
				return null;
			}, true);

		} catch (IllegalAccessException e) {
			// This should not happen even with errors while parsing
			e.printStackTrace();
		}

	}

	public boolean writeAuthentication() {
		if (authenticator == null) {
			authenticator = new DefaultAuthenticator();
		}
		if (parent.getGame().isClient()) {
			authenticator.w_client_auth(this, w_buffer);
		} else {
			return authenticator.w_server_auth(this, w_buffer);
		}
		return true;
	}

	public boolean readAuthentication() {
		if (authenticator == null) {
			authenticator = new DefaultAuthenticator();
		}
		if (Game.getGame().isClient()) {
			return authenticator.r_client_auth(this, r_buffer);
		} else {
			authenticator.r_server_auth(this, r_buffer);
		}
		return true;
	}

	public void requestPing() {
		w_buffer.putByte(Opcode.PINGPONG);
	}

	public void handlePing() {
		ping = true;
	}

	// Utility methods

	protected void invalid(byte opcode) {
		Logger.error(
				"Remote host sent an invalid operation (" + (opcode & 0xff) + ") : the connection will be terminated");
		error_log(r_buffer);
		throw new IllegalStateException();
	}

	protected void invalid_privilege(byte opcode) {
		Logger.warning("Remote client doesn't have enough permission to perform the following operation ("
				+ (opcode & 0xff) + ") : the connection will be terminated");
		error_log(r_buffer);
		throw new IllegalStateException();
	}

	protected void error_log(NetworkBuffer buffer) {
		Logger.error("Buffer position/limit/capacity: " + buffer.getPosition() + "/" + buffer.getLimit() + "/"
				+ buffer.getSize());
		try {
			Logger.error("Last byte: " + (buffer.getByteAt(buffer.getPosition() - 1) & 0xFF));
		} catch (Throwable t) {
		}
		Logger.error("Full buffer log:");
		String dump = new String();
		for (int i = 0; i < buffer.getLimit(); i++) {
			dump += (buffer.asArray()[i] & 0xFF) + (i == buffer.getLimit() - 1 ? "" : ".");
		}
		Logger.error(dump);
	}

	protected void recursive_flag(GameObject obj) {
		// Flag this object as deletable
		try {
			me_osetflag.invoke(obj, GameObject.DELETE, true);
		} catch (Throwable t) {
		}

		// Flag components
		obj.forEachComponent((id, component) -> {
			try {
				me_csetflag.invoke(component, GameComponent.DELETE, true);
			} catch (Throwable t) {
			}
		});

		// Flag children
		obj.forEachChild((id, child) -> {
			recursive_flag(child);
		});
	}

	protected void recursive_delete(GameObject obj) {
		// Check if flagged
		try {
			if ((boolean) me_ogetflag.invoke(obj, GameObject.DELETE)) {
				delete_list.add(obj);
				return;
			}
		} catch (Throwable t) {
		}

		// Check on components
		obj.forEachComponent((id, component) -> {
			try {
				if ((boolean) me_cgetflag.invoke(component, GameComponent.DELETE)) {
					delete_list.add(component);
				}
			} catch (Throwable t) {
			}
		});

		// Check on children
		obj.forEachChild((id, child) -> {
			recursive_delete(child);
		});
	}

	protected void perform_delete() {
		delete_list.forEach(object -> {
			if (GameObject.class.isAssignableFrom(object.getClass())) {
				((GameObject) object).destroy();
			} else {
				((GameComponent) object).destroy();
			}
		});
		delete_list.clear();
	}

	// Getters and setters

	public Game getGame() {
		return Game.getGame();
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
		return socket != null && !socket.isClosed() && ping;
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

	public long getLostConnectionTime() {
		return lostConnection;
	}

	public void setLostConnectionTime(long time) {
		this.lostConnection = time;
	}

	public CoreComponent getParent() {
		return parent;
	}

	public boolean isAwait() {
		return await;
	}

	public void setAwait(boolean val) {
		this.await = val;
	}

	public boolean isReplicationForced() {
		return forceReplication;
	}

	public void setForceReplication(boolean val) {
		forceReplication = val;
	}

}