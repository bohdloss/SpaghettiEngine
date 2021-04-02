package com.spaghetti.demo;

import com.spaghetti.core.GameObject;
import com.spaghetti.core.Level;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.physics.Physics;
import com.spaghetti.physics.RigidBody;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

public class Player extends GameObject {

	public Player(Level level) {
		super(level);
		addChild(new Mesh(level, Model.get("square"), Material.get("defaultMAT")));
		addChild(new Camera(level));
		addComponent(new RigidBody());
		level.getObject(Physics.class).setGravity(0, 0);
		addComponent(new PlayerController());
	}

}