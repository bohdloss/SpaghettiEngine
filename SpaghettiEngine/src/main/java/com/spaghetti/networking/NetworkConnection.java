package com.spaghetti.networking;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;

import com.spaghetti.core.*;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.GameEvent;
import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.*;
import com.spaghetti.objects.Camera;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.Utils;

public abstract class NetworkConnection {

	public static final class Identity {
		private Identity() {
		}
	}

	public static enum Priority {
		NONE, SEND, RECEIVE
	}

	// Static data
	protected static final Identity IDENTITY = new Identity();
	
	// "Id" fields
	protected static final Field f_oid = Utils.getPrivateField(GameObject.class, "id");
	protected static final Field f_cid = Utils.getPrivateField(GameComponent.class, "id");
	protected static final Field f_eid = Utils.getPrivateField(GameEvent.class, "id");
	protected static final Field f_rpcid = Utils.getPrivateField(RemoteProcedure.class, "id");
	
	// Flag management methods
	protected static final Method me_osetflag = Utils.getPrivateMethod(GameObject.class, "internal_setflag", int.class, boolean.class);
	protected static final Method me_csetflag = Utils.getPrivateMethod(GameComponent.class, "internal_setflag", int.class, boolean.class);
	protected static final Method me_ogetflag = Utils.getPrivateMethod(GameObject.class, "internal_getflag", int.class);
	protected static final Method me_cgetflag = Utils.getPrivateMethod(GameComponent.class, "internal_getflag", int.class);
	
	// RemoteProcedure related
	protected static final Field f_rpcready = Utils.getPrivateField(RemoteProcedure.class, "ready");
	protected static final Field f_rpcerror = Utils.getPrivateField(RemoteProcedure.class, "error");

	// Cache
	protected static final HashMap<String, Class<?>> m_clss = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameObject>> m_oconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameComponent>> m_cconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends GameEvent>> m_econstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<? extends RemoteProcedure>> m_rpcconstrs = new HashMap<>();
	protected static final HashMap<Class<?>, Field[]> m_fields = new HashMap<>();
	protected static final HashMap<Class<?>, Boolean> m_noreplicate = new HashMap<>();
	protected static final HashMap<Class<?>, Boolean> m_reliable = new HashMap<>();

	// Member data

	// Reference to owner
	protected CoreComponent parent;

	// Buffers
	protected NetworkBuffer w_buffer;
	protected NetworkBuffer r_buffer;

	// Interfaces
	protected Authenticator authenticator;
	protected HashMap<Class<?>, ClassReplicationRule> cls_rules;
	protected HashMap<Field, FieldReplicationRule> field_rules;

	// Flags
	public boolean forceReplication;
	public boolean reliable;
	protected boolean ping;
	public boolean goodbye;

	// Player info
	public GameObject player;
	public GameComponent player_controller;
	public Camera player_camera;

	// Cache
	protected final ArrayList<Object> delete_list = new ArrayList<>(256);
	protected final HashMap<Short, String> str_cache = new HashMap<>(256);

	public NetworkConnection(CoreComponent parent) {
		this.parent = parent;
		this.w_buffer = new NetworkBuffer(this, Game.getGame().getOptions().getEngineOption("networkbuffer"));
		this.r_buffer = new NetworkBuffer(this, Game.getGame().getOptions().getEngineOption("networkbuffer"));

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
	}

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
		disconnect();
	}

	public void provideAuthenticator(Authenticator authenticator) {
		this.authenticator = authenticator;
	}

	// Internal check functions and reflection utility
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

	@SuppressWarnings("unchecked")
	protected static Constructor<?> rpc_constr(Class<?> cls) throws NoSuchMethodException {
		Constructor<?> constructor = m_rpcconstrs.get(cls);
		if (constructor == null) {
			synchronized (m_rpcconstrs) {
				try {
					constructor = cls.getConstructor();
					constructor.setAccessible(true);
					m_rpcconstrs.put(cls, (Constructor<? extends RemoteProcedure>) constructor);
				} catch (NoSuchMethodException e) {
					Logger.error("Class " + cls.getName() + " must provide a default RemoteProcedure constructor");
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

	protected static boolean reliableCls(Class<?> cls) {
		Boolean res = m_reliable.get(cls);
		if (res == null) {
			synchronized (m_reliable) {
				res = cls.getAnnotation(Reliable.class) != null;
				m_reliable.put(cls, res);
			}
		}
		return res;
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

	protected boolean test_needReplication(Replicable replicable) {
		return replicable.needsReplication(this) || forceReplication;
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

	protected void writeReplicable(Replicable obj) {
		if (getGame().isClient()) {
			obj.writeDataClient(w_buffer);
		} else {
			obj.writeDataServer(w_buffer);
		}
	}

	protected void readReplicable(Replicable obj) {
		if (getGame().isClient()) {
			obj.readDataClient(r_buffer);
		} else {
			obj.readDataServer(r_buffer);
		}
	}

	// Read / Write interface

	public void parsePacket() throws Throwable {
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
				readLevelStructure();
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
				readObjectReplication();
				break;
			case Opcode.PINGPONG:
				handlePing();
				break;
			case Opcode.RPC:
				readRemoteProcedure();
				break;
			case Opcode.RPC_RESPONSE:
				readRemoteProcedureResponse();
				break;
			case Opcode.RPC_ACKNOWLEDGEMENT:
				readRPCAcknowledgement();
				break;
			case Opcode.OBJECTTREE:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readObjectTree(level);
				break;
			case Opcode.OBJECTDESTROY:
				if (getGame().isServer()) {
					invalid_privilege(opcode);
				}
				readObjectDestruction(level);
				break;
			case Opcode.GOODBYE:
				readGoodbye();
				break;
			}
		}
	}

	// Update custom level replication data
	public void writeLevelReplication(Level level) {
		if(!test_needReplication(level) || !test_writeClass(level)) {
			return;
		}
		
		
	}
	
	public void readLevelReplication(Level level) {
		
	}
	
	// Quick update on objects that need it
	public void writeObjectReplication() {
		Level level = getLevel();
		w_buffer.putByte(Opcode.DATA);

		// Write objects
		level.forEachActualObject((id, object) -> {
			if (test_writeClass(object) && test_needReplication(object)) {
				w_buffer.putByte(Opcode.ITEM);
				w_buffer.putInt(object.getId());
				int pos = w_buffer.getPosition();
				w_buffer.skip(Short.BYTES); // Allocate memory for skip destination
				writeReplicable(object);
				int off = w_buffer.getPosition() - (pos + Short.BYTES);
				w_buffer.putShortAt(pos, (short) off); // Write destination
			}
		});
		w_buffer.putByte(Opcode.STOP);

		// Write components
		level.forEachComponent((id, component) -> {
			if (test_writeClass(component) && test_needReplication(component)) {
				w_buffer.putByte(Opcode.ITEM);
				w_buffer.putInt(component.getId());
				int pos = w_buffer.getPosition();
				w_buffer.skip(Short.BYTES); // Allocate memory for skip destination
				writeReplicable(component);
				int off = w_buffer.getPosition() - (pos + Short.BYTES);
				w_buffer.putShortAt(pos, (short) off); // Write destination
			}
		});
		w_buffer.putByte(Opcode.STOP);
	}

	public void readObjectReplication() {
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
			readReplicable(object);
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
			readReplicable(component);
		}
	}

	// Serialization of level
	public void writeLevelStructure() {
		Level level = getLevel();
		reliable = true;

		// Write metadata
		w_buffer.putByte(Opcode.LEVEL);
		synchronized (level) {
			level.forEachObject(this::writeObjectStructure);
		}
		w_buffer.putByte(Opcode.STOP);
	}

	public void readLevelStructure() throws Throwable {
		Level level = getLevel();

		// First flag all objects as deletable
		// this will be reverted by readChildren
		// but only on objects that actually exist
		level.forEachObject(object -> recursive_flag(object, false));

		// Check for item flag
		while (r_buffer.getByte() == Opcode.ITEM) {
			readObjectStructure(level, (GameObject) null);
		}

		// If the objects are still flagged, it means
		// they no longer exist, therefore they will
		// be deleted
		level.forEachObject(this::recursive_delete);
		perform_delete();
	}

	// Initialization of objects
	protected void writeObjectStructure(GameObject obj) {
		if (noReplicate(obj.getClass())) {
			return;
		}
		forceReplication = true;

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

		obj.forEachChild((id, child) -> writeObjectStructure(child));
		w_buffer.putByte(Opcode.STOP); // Put stop flag on children
	}

	protected void readObjectStructure(Level level, GameObject parent) throws Throwable {
		int id = r_buffer.getInt(); // Get id of object
		String clazz = r_buffer.getString(true, NetworkBuffer.UTF_8); // Get class of object

		GameObject object = level.getObject(id);
		if (object == null) {
			// Retrieve the class and check if it is valid
			Class<?> objclass = cls(clazz);
			if (!GameObject.class.isAssignableFrom(objclass)) {
				throw new IllegalStateException("Invalid object class");
			}

			// Build a new instance of it
			object = (GameObject) o_constr(objclass).newInstance();

			// Set the id
			f_oid.set(object, id);

			// Register the object using the updater thread
			if (parent == null) {
				// Add to level directly if it has no parent
				final GameObject object_copy = object;
				getGame().getUpdaterDispatcher().quickQueue(() -> {
					level.addObject(object_copy);
					return null;
				});
			} else {
				// Add to appropriate parent instead
				final GameObject object_copy = object;
				getGame().getUpdaterDispatcher().quickQueue(() -> {
					parent.addChild(object_copy);
					return null;
				});
			}
		}
		// Remove DELETE flag, used later when checking for out-dated objects
		me_osetflag.invoke(object, GameObject.DELETE, false);

		// Check for item flag
		while (r_buffer.getByte() == Opcode.ITEM) {
			int comp_id = r_buffer.getInt(); // Get id of component
			String comp_clazz = r_buffer.getString(true, NetworkBuffer.UTF_8); // Get class of component

			GameComponent component = object.getComponent(comp_id);
			if (component == null) {
				// Retrieve the component class and check if it is valid
				Class<?> compclass = cls(comp_clazz);
				if (!GameComponent.class.isAssignableFrom(compclass)) {
					throw new IllegalStateException("Invalid component class");
				}

				// Build a new instance of it
				component = (GameComponent) c_constr(compclass).newInstance();

				// Set id
				f_cid.set(component, comp_id);

				// Add the component to its owner using the updater thread
				final GameObject object_copy = object;
				final GameComponent component_copy = component;
				getGame().getUpdaterDispatcher().quickQueue(() -> {
					object_copy.addComponent(component_copy);
					return null;
				});
			}
			// Again, used later to check for invalid components
			me_csetflag.invoke(component, GameComponent.DELETE, false);
		}

		// Recursively perform this on all children
		while (r_buffer.getByte() == Opcode.ITEM) {
			readObjectStructure(level, object);
		}
	}

	public void writeObjectTree(GameObject object) {
		reliable = true;
		w_buffer.putByte(Opcode.OBJECTTREE);
		w_buffer.putInt(object.getParent() == null ? -1 : object.getParent().getId());
		writeObjectStructure(object);
	}

	public void readObjectTree(Level level) throws Throwable {
		int parent_id = r_buffer.getInt();
		GameObject parent = parent_id == -1 ? null : level.getObject(parent_id);
		r_buffer.skip(1);

		// First flag all objects as deletable
		// this will be reverted by readChildren
		// but only on objects that actually exist
		recursive_flag(parent, true);

		readObjectStructure(level, parent);

		// If the objects are still flagged, it means
		// they no longer exist, therefore they will
		// be deleted
		level.forEachObject(this::recursive_delete);
		perform_delete();
	}

	// Destruction of objects
	public void writeObjectDestruction(GameObject object) {
		if (noReplicate(object.getClass())) {
			return;
		}
		reliable = true;
		w_buffer.putByte(Opcode.OBJECTDESTROY);
		w_buffer.putBoolean(false); // Component flag
		w_buffer.putInt(object.getId());
	}

	public void writeComponentDestruction(GameComponent component) {
		if (noReplicate(component.getClass())) {
			return;
		}
		reliable = true;
		w_buffer.putByte(Opcode.OBJECTDESTROY);
		w_buffer.putBoolean(true); // Component flag
		w_buffer.putInt(component.getId());
	}

	public void readObjectDestruction(Level level) {
		boolean isComp = r_buffer.getBoolean();
		int id = r_buffer.getInt();
		if (isComp) {
			GameComponent component = level.getComponent(id);
			if (component != null) {
				component.destroy();
			}
		} else {
			GameObject object = level.getObject(id);
			if (object != null) {
				object.destroy();
			}
		}
	}

	// Updates client's state
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
				parent.getGame().attachCamera(level_camera);
			}
		} else {
			player_camera = null;
			parent.getGame().detachCamera();
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
				parent.getGame().attachController(level_controller);
			}
		} else {
			player_controller = null;
			parent.getGame().detachController();
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

	// Serialization of single objects / components
	public void writeObject(GameObject obj) {
		if (!test_writeClass(obj) || !test_needReplication(obj)) {
			return;
		}
		w_buffer.putByte(Opcode.GAMEOBJECT);
		int pos = w_buffer.getPosition();
		w_buffer.skip(Integer.BYTES);
		w_buffer.putInt(obj.getId());
		// Let the object write custom data
		writeReplicable(obj);
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
		readReplicable(obj);
	}

	public void writeComponent(GameComponent comp) {
		if (!test_writeClass(comp) || test_needReplication(comp)) {
			return;
		}
		w_buffer.putByte(Opcode.GAMECOMPONENT);
		int pos = w_buffer.getPosition();
		w_buffer.skip(Integer.BYTES);
		w_buffer.putInt(comp.getId());
		// Let the component write custom data
		writeReplicable(comp);
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
		readReplicable(comp);
	}

	// Related to events and intentions
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
		if (!test_writeClass(event)) {
			// Sending this event is not allowed in this context!
			return;
		}
		if (reliableCls(event.getClass())) {
			reliable = true;
		}
		w_buffer.putByte(Opcode.GAMEEVENT);
		w_buffer.putInt(issuer == null ? -1 : issuer.getId());

		w_buffer.putString(event.getClass().getName());
		w_buffer.putInt(event.getId());
		writeReplicable(event);
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
			if (!test_readClass(event)) {
				// Receiving this event is not allowed in this context!
				return;
			}
			f_eid.set(event, event_id);
			EventDispatcher event_dispatcher = getGame().getEventDispatcher();
			FunctionDispatcher func_dispatcher = getGame().getUpdaterDispatcher();
			readReplicable(event);
			event.setFrom(getGame().isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

			func_dispatcher.queue(() -> {
				event_dispatcher.dispatchEvent(IDENTITY, issuer, event);
				return null;
			}, true);

		} catch (IllegalAccessException e) {
		}

	}

	// Authentication related
	public boolean writeAuthentication() {
		reliable = true;
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

	// Remote procedure calls related
	public void writeRemoteProcedure(RemoteProcedure rp) {
		if (!test_writeClass(rp)) {
			return;
		}
		if (reliableCls(rp.getClass())) {
			reliable = true;
		}
		RemoteProcedure.save(rp);

		w_buffer.putByte(Opcode.RPC);
		w_buffer.putInt(rp.getId());
		w_buffer.putString(true, rp.getClass().getName(), NetworkBuffer.UTF_8);
		rp.writeArgs(w_buffer);
	}

	public void readRemoteProcedure() throws ClassNotFoundException, InstantiationException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException {
		try {
			// Gather data and perform some checks
			int id = r_buffer.getInt();
			String clazz = r_buffer.getString(true, NetworkBuffer.UTF_8);
			Class<?> cls = cls(clazz);
			if (!RemoteProcedure.class.isAssignableFrom(cls)) {
				throw new IllegalStateException("Invalid RemoteProcedure class");
			}
			RemoteProcedure rp = (RemoteProcedure) rpc_constr(cls).newInstance();
			rp.readArgs(r_buffer);
			f_rpcid.set(rp, id);
			if (!test_readClass(rp)) {
				return;
			}

			// Dispatch RemoteProcedure execution to the updater thread
			getGame().getUpdaterDispatcher().queue(() -> {
				// Execute
				rp.execute(this);

				// Queue the function to write the callback
				CoreComponent core = getCore();

				if (ServerCore.class.isAssignableFrom(core.getClass())) {

					// For server
					((ServerCore) core).queueNetworkFunction(client -> {

						// Only in this specific client
						if (client == this) {
							client.writeRemoteProcedureResponse(rp);
						}
					});
				} else {

					// Same for client
					((ClientCore) core).queueNetworkFunction(client -> {

						// Only in this specific client
						if (client == this) {
							client.writeRemoteProcedureResponse(rp);
						}
					});
				}
				return null;
			}, true);
		} catch (IllegalAccessException e) {
		}
	}

	public void writeRemoteProcedureResponse(RemoteProcedure rpc) {
		if (reliableCls(rpc.getClass())) {
			reliable = true;
		}
		if (rpc.hasReturnValue()) {
			w_buffer.putByte(Opcode.RPC_RESPONSE);
			w_buffer.putInt(rpc.getId());
			rpc.writeReturn(w_buffer);
		} else {
			w_buffer.putByte(Opcode.RPC_ACKNOWLEDGEMENT);
			w_buffer.putInt(rpc.getId());
			w_buffer.putBoolean(rpc.isError());
		}
	}

	public void readRemoteProcedureResponse() throws Throwable {
		int id = r_buffer.getInt();
		RemoteProcedure rpc = RemoteProcedure.get(id);

		if (!rpc.hasReturnValue()) {
			f_rpcready.set(rpc, true);
			throw new IllegalStateException("RemoteProcedure " + rpc.getClass().getName() + " sent void instead of a return value");
		}

		rpc.readReturn(r_buffer);
		f_rpcready.set(rpc, true);
		getGame().getUpdaterDispatcher().queue(() -> rpc.executeReturnCallback());
	}

	public void readRPCAcknowledgement() throws Throwable {
		int id = r_buffer.getInt();
		RemoteProcedure rpc = RemoteProcedure.get(id);
		boolean error = r_buffer.getBoolean();

		if (rpc.hasReturnValue()) {
			f_rpcready.set(rpc, true);
			throw new IllegalStateException("RemoteProcedure " + rpc.getClass().getName() + " sent a return value instead of void");
		}

		f_rpcerror.set(rpc, error);
		f_rpcready.set(rpc, true);
		getGame().getUpdaterDispatcher().queue(() -> rpc.executeAckCallback());
	}

	// Ping related
	public void requestPing() {
		w_buffer.putByte(Opcode.PINGPONG);
	}

	public void handlePing() {
		ping = true;
	}

	public void writeGoodbye() {
		w_buffer.clear();
		w_buffer.putByte(Opcode.GOODBYE);
		goodbye = true;
	}

	public void readGoodbye() {
		goodbye = true;
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
		StringBuilder dump = new StringBuilder();
		for (int i = 0; i < buffer.getLimit(); i++) {
			dump.append(buffer.asArray()[i] & 0xFF);
			dump.append(i == buffer.getLimit() - 1 ? "" : ".");
		}
		Logger.error(dump.toString());
	}

	protected void recursive_flag(GameObject obj, boolean exclusive) {
		if (obj == null) {
			return;
		}
		if (!exclusive) {
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
		}

		// Flag children
		obj.forEachChild((id, child) -> {
			recursive_flag(child, false);
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

	public NetworkBuffer getWriteBuffer() {
		return w_buffer;
	}

	public NetworkBuffer getReadBuffer() {
		return r_buffer;
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public CoreComponent getCore() {
		return parent;
	}

	public boolean isForceReplication() {
		return forceReplication;
	}

	public void setForceReplication(boolean forceReplication) {
		this.forceReplication = forceReplication;
	}

	public boolean isReliable() {
		return reliable;
	}

	public void setReliable(boolean reliable) {
		this.reliable = reliable;
	}

	public boolean isPing() {
		return ping;
	}

	public void setPing(boolean ping) {
		this.ping = ping;
	}

	public Priority getPriority() {
		return Priority.NONE;
	}

	// Abstract methods
	public abstract void connect(Object socket);

	public abstract void connect(String ip, int port) throws Throwable;

	public abstract void disconnect();

	public void reconnect() throws Throwable {
		String ip = getRemoteIp();
		int port = getRemotePort();
		disconnect();
		connect(ip, port);
	}

	public abstract void send() throws Throwable;

	public abstract void receive() throws Throwable;

	// Abstract getters
	public abstract boolean isConnected();

	public abstract String getRemoteIp();

	public abstract int getRemotePort();

	public abstract String getLocalIp();

	public abstract int getLocalPort();

	public abstract boolean canSend();

	public abstract boolean canReceive();

}