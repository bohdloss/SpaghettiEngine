package com.spaghetti.input;

import java.util.ArrayList;
import java.util.HashMap;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.*;

public abstract class Controller<T extends GameObject> extends GameComponent {
	
	private static final class ControllerActionClass<U extends GameObject> {

		private ControllerAction<U> _interface;
		private Object[] params = new Object[0];
		
		public ControllerActionClass(ControllerAction<U> _interface) {
			this._interface = _interface;
		}
		
		// Interface
		
		@SuppressWarnings("unchecked")
		public void executeInterface(GameObject target) {
			_interface.execute((U) target, params);
		}

		
		// Getters and setters
		
		public ControllerAction<U> getInterface() {
			return _interface;
		}
		
		public void setParams(Object[] params) {
			if(this.params.length != params.length) {
				this.params = new Object[params.length];
			}
			
			for(int i = 0; i < params.length; i++) {
				this.params[i] = params[i];
			}
		}
		
	}
	
	private final HashMap<String, ControllerActionClass<T>> commands = new HashMap<>(256);
	private final ArrayList<ControllerActionClass<T>> queue = new ArrayList<>(256);
	
	@Override
	public void commonUpdate(float delta) {
		for(ControllerActionClass<T> action : queue) {
				action.executeInterface(getOwner());
		}
		queue.clear();
	}
	
	// Managing commands
	
	public void registerCommand(String name, ControllerAction<T> command) {
		commands.put(name, new ControllerActionClass<T>(command));
	}
	
	public void unregisterCommand(String name) {
		commands.remove(name);
	}
	
	public void registerCommands(String name, ControllerAction<T> command, ControllerAction<T> opposite) {
		registerCommand(name, command);
		registerCommand("!" + name, opposite);
	}
	
	public void unregisterCommands(String name) {
		unregisterCommand(name);
		unregisterCommand("!" + name);
	}
	
	public ControllerAction<T> getCommand(String name) {
		return commands.get(name).getInterface();
	}
	
	public boolean isCommandRegistered(String name) {
		return commands.containsKey(name);
	}
	
	public boolean isCommandRegistered(ControllerAction<T> command) {
		return commands.containsValue(new ControllerActionClass<T>(command));
	}
	
	public void execCommand(String name) {
		ControllerActionClass<T> action = commands.get(name);
		if(action == null || queue.contains(action)) {
			return;
		}
		action.executeInterface(getOwner());
	}
	
	public void execCommandWithParams(String name, Object...params) {
		ControllerActionClass<T> action = commands.get(name);
		if(action == null || queue.contains(action)) {
			return;
		}
		action.setParams(params);
		action.executeInterface(getOwner());
	}
	
	public void queueCommand(String name) {
		ControllerActionClass<T> action = commands.get(name);
		if(action == null || queue.contains(action)) {
			return;
		}
		queue.add(action);
	}
	
	public void queueCommandWithParams(String name, Object...params) {
		ControllerActionClass<T> action = commands.get(name);
		if(action == null || queue.contains(action)) {
			return;
		}
		action.setParams(params);
		queue.add(action);
	}
	
}