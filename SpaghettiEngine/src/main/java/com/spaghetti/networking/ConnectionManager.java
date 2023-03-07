package com.spaghetti.networking;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import com.spaghetti.core.Game;
import com.spaghetti.utils.ReflectionUtil;
import com.spaghetti.world.GameComponent;
import com.spaghetti.world.GameObject;
import com.spaghetti.world.Level;
import com.spaghetti.events.EventDispatcher;
import com.spaghetti.events.GameEvent;
import com.spaghetti.utils.FunctionDispatcher;
import com.spaghetti.utils.Logger;
import com.spaghetti.utils.ThreadUtil;

public class ConnectionManager {

	public static final class Identity {
		private Identity() {
		}
	}

	// Static data
	protected static final Identity IDENTITY = new Identity();

	// "Id" fields
	protected static final Field f_oid = ReflectionUtil.getPrivateField(GameObject.class, "id");
	protected static final Field f_cid = ReflectionUtil.getPrivateField(GameComponent.class, "id");
	protected static final Field f_eid = ReflectionUtil.getPrivateField(GameEvent.class, "id");
	protected static final Field f_rpcid = ReflectionUtil.getPrivateField(RemoteProcedure.class, "id");

	// Flag management methods
	protected static final Method me_osetflag = ReflectionUtil.getPrivateMethod(GameObject.class, "internal_setflag", int.class,
			boolean.class);
	protected static final Method me_csetflag = ReflectionUtil.getPrivateMethod(GameComponent.class, "internal_setflag",
			int.class, boolean.class);
	protected static final Method me_ogetflag = ReflectionUtil.getPrivateMethod(GameObject.class, "internal_getflag", int.class);
	protected static final Method me_cgetflag = ReflectionUtil.getPrivateMethod(GameComponent.class, "internal_getflag",
			int.class);

	// RemoteProcedure related
	protected static final Field f_rpcready = ReflectionUtil.getPrivateField(RemoteProcedure.class, "ready");
	protected static final Field f_rpcerror = ReflectionUtil.getPrivateField(RemoteProcedure.class, "error");

	// Cache
	protected static final HashMap<String, Class<?>> m_clss = new HashMap<>();
	protected static final HashMap<Class<?>, Constructor<?>> m_constructors = new HashMap<>();

	// Member data

	// Reference to owner
	protected NetworkCore core;
	protected ConnectionEndpoint endpoint;
	protected NetworkBuffer writeBuffer, readBuffer;

	// Flags
	public boolean forceReplication;

	// Player info
	public GameObject player;

	// Cache
	protected ArrayList<Object> delete_cache = new ArrayList<>(256);
	protected HashMap<Short, String> str_cache = new HashMap<>(256);
	protected HashMap<Integer, RemoteProcedure> rpc_cache = new HashMap<>(256);

	public ConnectionManager(NetworkCore core) {
		this.core = core;
	}

	public void destroy() {
		this.delete_cache.clear();
		this.delete_cache = null;
		this.str_cache.clear();
		this.str_cache = null;
		this.rpc_cache.clear();
		this.rpc_cache = null;
	}

	// Static functions to safely retrieve cache

	protected static Class<?> cachedClass(String name) throws ClassNotFoundException {
		Class<?> cls = m_clss.get(name);
		if (cls == null) {
			synchronized (m_clss) {
				try {
					cls = Class.forName(name, false, ConnectionManager.class.getClassLoader());
				} catch (ClassNotFoundException e) {
					Logger.error("Class not found: " + name);
					throw e;
				}
				m_clss.put(name, cls);
			}
		}
		return cls;
	}

	protected static Constructor<?> cachedConstructor(Class<?> cls) throws NoSuchMethodException {
		Constructor<?> constructor = m_constructors.get(cls);
		if (constructor == null) {
			synchronized (m_constructors) {
				try {
					constructor = cls.getConstructor();
					constructor.setAccessible(true);
					m_constructors.put(cls, constructor);
				} catch (NoSuchMethodException e) {
					Logger.error("Class " + cls.getName() + " must provide a constructor with no arguments");
					throw e;
				}
			}
		}
		return constructor;
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<? extends GameObject> cachedGOConstructor(Class<?> cls) throws NoSuchMethodException {
		return (Constructor<? extends GameObject>) cachedConstructor(cls);
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<? extends GameComponent> cachedGCConstructor(Class<?> cls)
			throws NoSuchMethodException {
		return (Constructor<? extends GameComponent>) cachedConstructor(cls);
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<? extends GameEvent> cachedGEConstructor(Class<?> cls) throws NoSuchMethodException {
		return (Constructor<? extends GameEvent>) cachedConstructor(cls);
	}

	@SuppressWarnings("unchecked")
	protected static Constructor<? extends RemoteProcedure> cachedRPConstructor(Class<?> cls)
			throws NoSuchMethodException {
		return (Constructor<? extends RemoteProcedure>) cachedConstructor(cls);
	}

	// Internal utility

	protected boolean needsReplication(Replicable replicable) {
		return (replicable.needsReplication(this) || forceReplication) && !replicable.isLocal();
	}

	protected void writeReplicable(Replicable obj) {
		if (getGame().isClient()) {
			obj.writeDataClient(this, writeBuffer);
		} else {
			obj.writeDataServer(this, writeBuffer);
		}
	}

	protected void readReplicable(Replicable obj) {
		if (getGame().isClient()) {
			obj.readDataClient(this, readBuffer);
		} else {
			obj.readDataServer(this, readBuffer);
		}
	}

	// Read / Write interfaces

	public void parsePacket() throws Throwable {
		byte opcode;
		while ((opcode = readBuffer.getByte()) != Opcode.END) {
			switch (opcode) {
			default:
				invalid(opcode);
				return;
			case Opcode.LEVEL:
				readLevelStructure();
				break;
			case Opcode.GAMEOBJECT:
				readObjectReplication();
				break;
			case Opcode.GAMECOMPONENT:
				readComponentReplication();
				break;
			case Opcode.GAMEEVENT:
				readGameEvent();
				break;
			case Opcode.DATA:
				readCompleteReplication();
				break;
			case Opcode.REMOTEPROCEDURE:
				readRemoteProcedure();
				break;
			case Opcode.RP_RESPONSE:
				readRemoteProcedureResponse();
				break;
			case Opcode.RP_ACKNOWLEDGEMENT:
				readRPCAcknowledgement();
				break;
			case Opcode.OBJECT_TREE:
				readObjectTree();
				break;
			case Opcode.OBJECT_DESTROY:
				readObjectDestruction();
				break;
			}
		}
	}

	// Update custom level replication data
	public void writeLevelReplication(Level level) {

	}

	public void readLevelReplication(Level level) {

	}

	// Quick update on objects that need it
	public void writeCompleteReplication() {
		Level level = getLevel();
		writeBuffer.putByte(Opcode.DATA);

		// Write objects
		level.forEachActualObject((id, object) -> {

			// Should we write?
			if (needsReplication(object)) {

				// Write binary metadata
				writeBuffer.putInt(object.getId());
				int pos = writeBuffer.getPosition();
				writeBuffer.skip(Short.BYTES); // Allocate memory for skip destination

				// Write object data
				writeReplicable(object);

				// More metadata
				int off = writeBuffer.getPosition() - pos - Short.BYTES;
				writeBuffer.putShortAt(pos, (short) off); // Write destination
			}
		});
		writeBuffer.putInt(-1);

		// Write components
		level.forEachComponent((id, component) -> {

			// Should we write?
			if (needsReplication(component)) {

				// Write binary metadata
				writeBuffer.putInt(component.getId());
				int pos = writeBuffer.getPosition();
				writeBuffer.skip(Short.BYTES); // Allocate memory for skip destination

				// Write component data
				writeReplicable(component);

				// More metadata
				int off = writeBuffer.getPosition() - (pos + Short.BYTES);
				writeBuffer.putShortAt(pos, (short) off); // Write destination
			}

		});

		writeBuffer.putInt(-1);
	}

	public void readCompleteReplication() {
		Level level = getLevel();

		// Read objects
		int id = -1;
		while ((id = readBuffer.getInt()) != -1) {

			// Read metadata and validate
			short skip = readBuffer.getShort();
			if (skip < 0) {
				throw new IllegalStateException("Negative skip value");
			}
			GameObject object = level.getObject(id);

			// Do we skip?
			if (object == null) {
				readBuffer.skip(skip);
				continue;
			}

			// Read!
			readReplicable(object);
		}

		// Read components
		while ((id = readBuffer.getInt()) != -1) {

			// Get and validate metadata
			short skip = readBuffer.getShort();
			if (skip < 0) {
				throw new IllegalStateException("Negative skip value");
			}
			GameComponent component = level.getComponent(id);

			// Shall we skip?
			if (component == null) {
				readBuffer.skip(skip);
				continue;
			}

			// Read
			readReplicable(component);
		}
	}

	// Serialization of level
	public void writeLevelStructure() {
		if (getGame().isClient()) {
			throw new IllegalStateException("Clients can't send level structure");
		}
		Level level = getLevel();

		// Write metadata
		writeBuffer.putByte(Opcode.LEVEL);
		level.forEachObject(this::writeObjectStructure);
		writeBuffer.putByte(Opcode.STOP);
	}

	public void readLevelStructure() throws Throwable {
		if (getGame().isServer()) {
			throw new IllegalStateException("Servers can't receive level structure");
		}
		Level level = getLevel();

		// First flag all objects as deletable
		// this will be reverted by readChildren
		// but only on objects that actually exist
		level.forEachObject(object -> recursive_flag(object, false));

		// Check for item flag
		while (readBuffer.getByte() == Opcode.ITEM) {
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
		// We shouldn't write this object
		if (obj.isLocal()) {
			return;
		}
		endpoint.reliable = true;
		forceReplication = true;

		writeBuffer.putInt(obj.getId()); // Put id of the object
		writeBuffer.putString(true, obj.getClass().getName(), NetworkBuffer.UTF_8); // Put class of the object

		obj.forEachComponent((id, component) -> {

			// We should write the component
			if (!component.isLocal()) {
				writeBuffer.putInt(component.getId()); // Put id of component
				writeBuffer.putString(true, component.getClass().getName(), NetworkBuffer.UTF_8); // Put class of component
			}
		});
		writeBuffer.putInt(-1); // Put stop flag on components

		obj.forEachChild((id, child) -> writeObjectStructure(child));
		writeBuffer.putInt(-1); // Put stop flag on children
	}

	protected void readObjectStructure(Level level, GameObject parent) throws Throwable {
		// Get class name and id of the object
		int id = readBuffer.getInt();
		String clazz = readBuffer.getString(true, NetworkBuffer.UTF_8);

		// Retrieve the class and check if it is valid
		Class<?> objclass = cachedClass(clazz);
		if (!GameObject.class.isAssignableFrom(objclass)) {
			throw new IllegalStateException("Invalid GameObject class");
		}

		// Attempt to retrieve object from level
		GameObject object = level.getObject(id);

		// Failed, create it
		if (object == null) {

			// Build a new instance of it
			object = cachedGOConstructor(objclass).newInstance();

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

		// Iterate through components
		int comp_id = -1;
		while ((comp_id = readBuffer.getInt()) != -1) {

			// Get id and class name of the component
			String comp_clazz = readBuffer.getString(true, NetworkBuffer.UTF_8);

			// Retrieve the component class and check if it is valid
			Class<?> compclass = cachedClass(comp_clazz);
			if (!GameComponent.class.isAssignableFrom(compclass)) {
				throw new IllegalStateException("Invalid GameComponent class");
			}

			// Attempt to retrieve component from level
			GameComponent component = object.getComponent(comp_id);

			// Failed
			if (component == null) {

				// Build a new instance of it
				component = cachedGCConstructor(compclass).newInstance();

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
		while (readBuffer.getInt() != -1) {
			readBuffer.skip(-Integer.BYTES);
			readObjectStructure(level, object);
		}
	}

	public void writeObjectTree(GameObject object) {
		// Any object with a local parent cannot be replicated
		GameObject parent = object;
		while(parent != null) {
			if(parent.isLocal()) {
				return;
			}
			parent = parent.getParent();
		}

		endpoint.reliable = true;
		writeBuffer.putByte(Opcode.OBJECT_TREE);
		writeBuffer.putInt(object.getParent() == null ? -1 : object.getParent().getId());
		writeObjectStructure(object);
	}

	public void readObjectTree() throws Throwable {
		Level level = getLevel();
		
		int parent_id = readBuffer.getInt();
		GameObject parent = (parent_id == -1) ? null : level.getObject(parent_id);

		// Retrieve class in advance for security checks
		readBuffer.skip(Integer.BYTES); // Skip object id
		String clazz = readBuffer.getString(true, NetworkBuffer.UTF_8);

		// Retrieve the class and check if it is valid
		Class<?> objclass = cachedClass(clazz);
		if (!GameObject.class.isAssignableFrom(objclass)) {
			throw new IllegalStateException("Invalid object class");
		}

		// Set back position
		readBuffer.skip(-Integer.BYTES);

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
		// Any object with a local parent cannot be replicated
		GameObject parent = object;
		while(parent != null) {
			if(parent.isLocal()) {
				return;
			}
			parent = parent.getParent();
		}

		endpoint.reliable = true;
		writeBuffer.putByte(Opcode.OBJECT_DESTROY);
		writeBuffer.putBoolean(false); // Component flag
		writeBuffer.putInt(object.getId());
	}

	public void writeComponentDestruction(GameComponent component) {
		// Any component that is local or has a local parent cannot be replicated
		if (component.isLocal()) {
			return;
		}
		GameObject parent = component.getOwner();
		while(parent != null) {
			if(parent.isLocal()) {
				return;
			}
			parent = parent.getParent();
		}


		endpoint.reliable = true;
		writeBuffer.putByte(Opcode.OBJECT_DESTROY);
		writeBuffer.putBoolean(true); // Component flag
		writeBuffer.putInt(component.getId());
	}

	public void readObjectDestruction() {
		Level level = getLevel();
		
		// Retrieve component boolean and id
		boolean isComp = readBuffer.getBoolean();
		int id = readBuffer.getInt();

		// No common interface for both components and objects
		if (isComp) {

			// Attempt to retrieve component
			GameComponent component = level.getComponent(id);
			if (component != null) {

				// Perform destroy
				getGame().getUpdaterDispatcher().quickQueue(() -> {
					component.destroy();
					return null;
				});
			}
		} else {

			// Attempt to retrieve object
			GameObject object = level.getObject(id);
			if (object != null) {

				// Perform destroy
				getGame().getUpdaterDispatcher().quickQueue(() -> {
					object.destroy();
					return null;
				});
			}
		}
	}

	// Serialization of single objects / components
	public void writeObjectReplication(GameObject obj) {
		if (!needsReplication(obj)) {
			return;
		}

		// Metadata
		writeBuffer.putByte(Opcode.GAMEOBJECT);
		writeBuffer.putInt(obj.getId());
		int pos = writeBuffer.getPosition();
		writeBuffer.skip(Short.BYTES); // Allocate memory for skip destination

		// Perform write
		writeReplicable(obj);

		// More metadata
		int off = writeBuffer.getPosition() - pos - Short.BYTES;
		writeBuffer.putShortAt(pos, (short) off);
	}

	public void readObjectReplication() {
		Level level = getLevel();
		
		// Read metadata
		int id = readBuffer.getInt();
		short skip = readBuffer.getShort();
		if(skip < 0) {
			throw new IllegalStateException("Negative skip value");
		}

		// Attempt to retrieve object
		GameObject obj = level.getObject(id);

		// Failed
		if (obj == null || obj.isLocal()) {
			readBuffer.skip(skip);
			return;
		}

		// Perform read
		readReplicable(obj);
	}

	public void writeComponentReplication(GameComponent comp) {
		if (!needsReplication(comp)) {
			return;
		}

		// Metadata
		writeBuffer.putByte(Opcode.GAMEOBJECT);
		writeBuffer.putInt(comp.getId());
		int pos = writeBuffer.getPosition();
		writeBuffer.skip(Short.BYTES); // Allocate memory for skip destination

		// Perform write
		writeReplicable(comp);

		// More metadata
		int off = writeBuffer.getPosition() - pos - Short.BYTES;
		writeBuffer.putShortAt(pos, (short) off);
	}

	public void readComponentReplication() {
		Level level = getLevel();
		
		// Read metadata
		int id = readBuffer.getInt();
		short skip = readBuffer.getShort();
		if(skip < 0) {
			throw new IllegalStateException("Negative skip value");
		}

		// Attempt to retrieve object
		GameComponent comp = level.getComponent(id);

		// Failed
		if (comp == null || comp.isLocal()) {
			readBuffer.skip(skip);
			return;
		}

		// Perform read
		readReplicable(comp);
	}

	// Game events

	public void writeGameEvent(GameEvent event) {
		if (event.isLocal()) {
			return;
		}
		writeBuffer.putByte(Opcode.GAMEEVENT);

		writeBuffer.putString(event.getClass().getName());
		writeBuffer.putInt(event.getId());
		writeReplicable(event);
	}

	public void readGameEvent() throws Throwable {
		// Obtain event metadata
		String event_class = readBuffer.getString();
		int event_id = readBuffer.getInt();

		// Check validity of the class
		Class<?> eventclass = cachedClass(event_class);
		if (!GameEvent.class.isAssignableFrom(eventclass)) {
			throw new IllegalStateException("Invalid GameEvent class");
		}

		// Allocate event
		GameEvent event = cachedGEConstructor(eventclass).newInstance();
		f_eid.set(event, event_id);

		// Retrieve event and function dispatchers
		EventDispatcher event_dispatcher = getGame().getEventDispatcher();
		FunctionDispatcher func_dispatcher = getGame().getUpdaterDispatcher();

		// Read event data
		readReplicable(event);

		// Finish event initialization
		event.setFrom(getGame().isClient() ? GameEvent.SERVER : GameEvent.CLIENT);

		// Dispatch event
		func_dispatcher.queue(() -> {
			event_dispatcher.dispatchEvent(IDENTITY, event);
			return null;
		}, true);
	}

	// Remote procedure calls
	public void writeRemoteProcedure(RemoteProcedure rpc) {
		// Is this reliable?
		endpoint.reliable |= rpc.isReliable();

		// Cache this for the moment we receive a callback
		rpc_cache.put(rpc.getId(), rpc);

		// Write metadata
		writeBuffer.putInt(rpc.getId());
		writeBuffer.putString(true, rpc.getClass().getName(), NetworkBuffer.UTF_8);

		// Write arguments
		rpc.writeArgs(writeBuffer);
	}

	public void readRemoteProcedure() throws Throwable {
		// Get some metadata
		int id = readBuffer.getInt();
		String clazz = readBuffer.getString(true, NetworkBuffer.UTF_8);

		// Check if class is valid
		Class<?> cls = cachedClass(clazz);
		if (!RemoteProcedure.class.isAssignableFrom(cls)) {
			throw new IllegalStateException("Invalid RemoteProcedure class");
		}

		// Allocate procedure
		RemoteProcedure rpc = cachedRPConstructor(cls).newInstance();
		f_rpcid.set(rpc, id);

		// Read arguments
		rpc.readArgs(readBuffer);

		// Dispatch RemoteProcedure execution to the updater thread
		getGame().getUpdaterDispatcher().queue(() -> {
			// Execute
			rpc.execute(this);

			// Queue the function to write the callback
			getGame().getNetworkManager().queueNetworkFunction(client -> client.writeRemoteProcedureResponse(rpc));
			return null;
		}, true);
	}

	public void writeRemoteProcedureResponse(RemoteProcedure rpc) {
		endpoint.reliable |= rpc.isReliable();

		if (rpc.hasReturnValue()) {
			// Write response metadata
			writeBuffer.putByte(Opcode.RP_RESPONSE);
			writeBuffer.putInt(rpc.getId());

			// Write return value
			rpc.writeReturn(writeBuffer);
		} else {
			// Write acknowledgement metadata
			writeBuffer.putByte(Opcode.RP_ACKNOWLEDGEMENT);
			writeBuffer.putInt(rpc.getId());
			writeBuffer.putBoolean(rpc.isError()); // Needed explicitly here
		}
	}

	public void readRemoteProcedureResponse() throws Throwable {
		// Get metadata
		int id = readBuffer.getInt();

		// Obtain saved procedure
		RemoteProcedure rpc = rpc_cache.remove(id);

		// Failed
		if (rpc == null) {
			throw new IllegalStateException("Received response for unknown procedure");
		}

		// We shouldn't receive a return value
		if (!rpc.hasReturnValue()) {
			f_rpcready.set(rpc, true); // Can be reused
			throw new IllegalStateException("Remote procedure sent a return value instead of void");
		}

		// Read return value
		rpc.readReturn(readBuffer);
		f_rpcready.set(rpc, true); // Can be reused

		// Queue callback to updater thread
		getGame().getUpdaterDispatcher().queue(() -> rpc.executeReturnCallback());
	}

	public void readRPCAcknowledgement() throws Throwable {
		// Read metadata
		int id = readBuffer.getInt();

		// Retrieve saved procedure
		RemoteProcedure rpc = rpc_cache.remove(id);

		// Shouldn't have return value
		if (rpc.hasReturnValue()) {
			f_rpcready.set(rpc, true); // Can be reused
			throw new IllegalStateException("Remote procedure returned void but should have returned a value");
		}

		// Error occurred remotely?
		boolean error = readBuffer.getBoolean();

		// Set flags
		f_rpcerror.set(rpc, error);
		f_rpcready.set(rpc, true);

		// Queue callback to updater
		getGame().getUpdaterDispatcher().queue(() -> rpc.executeAckCallback());
	}

	// Utility methods

	protected void invalid(byte opcode) {
		Logger.error(
				"Remote host sent an invalid operation (" + (opcode & 0xff) + ") : the connection will be terminated");
		error_log(readBuffer);
		throw new IllegalStateException();
	}

	protected void invalid_privilege(byte opcode) {
		Logger.warning("Remote client doesn't have enough permission to perform the following operation ("
				+ (opcode & 0xff) + ") : the connection will be terminated");
		error_log(readBuffer);
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

	protected void recursive_flag(GameObject obj, boolean ignoreTop) {
		if (obj == null) {
			return;
		}
		if (!ignoreTop) {
			// Flag this object as deletable
			try {
				if (true) {
					me_osetflag.invoke(obj, GameObject.DELETE, true);
				}
			} catch (Throwable t) {
			}

			// Flag components
			obj.forEachComponent((id, component) -> {
				try {
					if (true) {
						me_csetflag.invoke(component, GameComponent.DELETE, true);
					}
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
				delete_cache.add(obj);
				return;
			}
		} catch (Throwable t) {
		}

		// Check on components
		obj.forEachComponent((id, component) -> {
			try {
				if ((boolean) me_cgetflag.invoke(component, GameComponent.DELETE)) {
					delete_cache.add(component);
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
		long func = getGame().getUpdaterDispatcher().queue(() -> {
			delete_cache.forEach(object -> {
				if (GameObject.class.isAssignableFrom(object.getClass())) {
					((GameObject) object).destroy();
				} else {
					((GameComponent) object).destroy();
				}
			});
			delete_cache.clear();
			return null;
		});
		getGame().getUpdaterDispatcher().waitFor(func);
	}

	// Getters and setters

	public Game getGame() {
		return getCore().getGame();
	}

	public Level getLevel() {
		return player.getLevel();
	}

	public NetworkCore getCore() {
		return core;
	}

	public boolean isForceReplication() {
		return forceReplication;
	}

	public void setForceReplication(boolean forceReplication) {
		this.forceReplication = forceReplication;
	}

	public void setEndpoint(ConnectionEndpoint endpoint) {
		if(endpoint != null) {
			writeBuffer = endpoint.writeBuffer;
			readBuffer = endpoint.readBuffer;
		}
		this.endpoint = endpoint;
	}

	public ConnectionEndpoint getEndpoint() {
		return endpoint;
	}

}