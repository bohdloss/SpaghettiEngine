package com.spaghetti.input;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.spaghetti.utils.HashUtil;
import com.spaghetti.world.GameComponent;
import com.spaghetti.world.GameObject;
import com.spaghetti.networking.ConnectionManager;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.utils.ThreadUtil;

public class Controller<T extends GameObject> extends GameComponent {

	private final HashMap<Integer, ControllerAction<T>> commands = new HashMap<>(256);
	private final ArrayList<ControllerAction<T>> queue = new ArrayList<>(256);
	private final ArrayList<Integer> networkQueue = new ArrayList<>(256);

	@SuppressWarnings("unchecked")
	@Override
	public void commonUpdate(float delta) {
		synchronized (queue) {
			for (ControllerAction<T> action : queue) {
				action.execute((T) getOwner());
			}
			queue.clear();
		}
	}

	// Managing commands

	public void registerCommand(String name, ControllerAction<T> command) {
		commands.put(HashUtil.intHash(name), command);
	}

	public void unregisterCommand(String name) {
		commands.remove(HashUtil.intHash(name));
	}

	public void registerCommands(String name, ControllerAction<T> command, ControllerAction<T> opposite) {
		registerCommand("+" + name, command);
		registerCommand("-" + name, opposite);
	}

	public void unregisterCommands(String name) {
		unregisterCommand("+" + name);
		unregisterCommand("-" + name);
	}

	public ControllerAction<T> getCommand(String name) {
		return commands.get(HashUtil.intHash(name));
	}

	public boolean isCommandRegistered(String name) {
		return commands.containsKey(HashUtil.intHash(name));
	}

	public boolean isCommandRegistered(ControllerAction<T> command) {
		return commands.containsValue(command);
	}

	@SuppressWarnings("unchecked")
	public void execCommand(Integer command) {
		ControllerAction<T> action = commands.get(command);
		if (action == null) {
			return;
		}
		action.execute((T) getOwner());

		synchronized (networkQueue) {
			networkQueue.add(command);
		}
	}

	public void queueCommand(Integer command) {
		synchronized (queue) {
			ControllerAction<T> action = commands.get(command);
			if (action == null) {
				return;
			}
			queue.add(action);
		}

		synchronized (networkQueue) {
			networkQueue.add(command);
		}
	}

	// Network interface

	public boolean needsReplication() {
		return networkQueue.size() > 0;
	}

	@Override
	public void writeDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		doWrite(buffer);
	}

	@Override
	public void writeDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		doWrite(buffer);
	}

	@Override
	public void readDataClient(ConnectionManager manager, NetworkBuffer buffer) {
		doRead(buffer);
	}

	@Override
	public void readDataServer(ConnectionManager manager, NetworkBuffer buffer) {
		doRead(buffer);
	}

	protected void doWrite(NetworkBuffer buffer) {
		synchronized (networkQueue) {
			byte size = (byte) (networkQueue.size() > Byte.MAX_VALUE ? Byte.MAX_VALUE : networkQueue.size());
			buffer.putByte(size);
			Iterator<Integer> iter = networkQueue.iterator();
			byte i = size;
			while (i < size) {
				buffer.putInt(iter.next());
				iter.remove();
				i++;
			}
		}
	}

	protected void doRead(NetworkBuffer buffer) {
		synchronized (queue) {
			byte size = buffer.getByte();
			for (byte i = 0; i < size; i++) {
				int action = buffer.getInt();
				queue.add(commands.get(action));
			}
		}
	}

}