package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.lwjgl.glfw.GLFW;

public final class Game {

	//Support for multiple instances of the engine
	//in the same java process
	private static ArrayList<Game> games = new ArrayList<Game>();
	private static HashMap<Long, Integer> links = new HashMap<Long, Integer>();
	
	public static void init() {
		GLFW.glfwInit();
	}	
	
	public static Game getGame() {
		Long id = Thread.currentThread().getId();
		Integer index = links.get(id);
		if(index != null) {
			if(index >= 0 && index < games.size()) {
				return games.get(index);
			}
		}
		return null;
	}
	
	public static FunctionDispatcher getDispatcher() {
		Game game = getGame();
		if(game!=null) {
			return game.dispatcher;
		}
		return null;
	}
	
	public static GameWindow getWindow() {
		Game game = getGame();
		if(game!=null) {
			return game.window;
		}
		return null;
	}
	
	public static Updater getUpdater() {
		Game game = getGame();
		if(game!=null) {
			return game.updater;
		}
		return null;
	}
	
	public static Renderer getRenderer() {
		Game game = getGame();
		if(game!=null) {
			return game.renderer;
		}
		return null;
	}
	
	//Instance globals
	private FunctionDispatcher dispatcher;
	private GameWindow window;
	
	private Updater updater;
	private Renderer renderer;

	private int index;
	
	//Constructors using custom classes
	public Game(GameWindow window, Class<? extends Updater> updater, Class<? extends Renderer> renderer) throws Exception {
		this.dispatcher = new FunctionDispatcher(Thread.currentThread().getId());
		this.window=window;
		
		games.add(this);
		this.index = games.indexOf(this);
		
		if(updater != null) {
			this.updater = updater.getConstructor(Game.class).newInstance(this);
			links.put(this.updater.getId(), index);
		}
		if(renderer != null) {
			this.renderer = renderer.getConstructor(Game.class).newInstance(this);
			links.put(1l, index);
		}
	}
	
	public void registerThread(long id) {
		if(links.get(id) == null) {
			links.put(id, index);
		}
	}
	
	public void registerThread() {
		long id = Thread.currentThread().getId();
		registerThread(id);
	}
	
	public void unregisterThread(long id) {
		if(links.get(id) != null && links.get(id) == index) {
			links.remove(id);
		}
	}
	
	public void unregisterThread() {
		long id = Thread.currentThread().getId();
		unregisterThread(id);
	}
	
	public void begin() {
		if(updater!=null) updater.start();
		if(window!=null) window.setVisible(true);
		
		loop();
	}

	private void loop() {
		while(true) {
			dispatcher.computeEvents();
			GameWindow.pollEvents();
			
			if(renderer!=null) renderer.update(0);
			
			if(window!=null) {
				window.update(0);
				window.swap();
				if(window.shouldClose()) break;
			}
		}
	}
	
}
