package com.spaghetti.demo;

import com.spaghetti.core.GameObject;
import com.spaghetti.objects.Camera;
import com.spaghetti.objects.Mesh;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

public class Player extends GameObject {

	public Player() {
		addChild(new Mesh(Model.get("square"), Material.get("defaultMAT")));
		addChild(new Camera());
		getChild(Camera.class).setFov(20);
		addComponent(new PlayerController());
	}

	@Override
	protected void onBeginPlay() {
		getLevel().attachCamera(getChild(Camera.class));
	}

}