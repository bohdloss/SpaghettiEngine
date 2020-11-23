package com.spaghettiengine.core;

import java.util.ArrayList;
import java.util.HashMap;

import org.joml.Vector2d;

import com.spaghettiengine.render.*;

public class Level implements Tickable {

	protected Game source;
	protected ArrayList<GameComponent> components = new ArrayList<GameComponent>();
	protected HashMap<Long, GameComponent> ordered = new HashMap<Long, GameComponent>();
	protected Camera activeCamera;	
	
	//Physics related
	
	protected Vector2d gravity = new Vector2d();
	
	public Level(Game source) {
		this.source=source;
	}
	
	public void addComponent(GameComponent component) {
		
	}

	@Override
	public void update(float delta) {
		components.forEach((component)->{
			component.update(delta);
		});
	}
	
}
