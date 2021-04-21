package com.spaghetti.demo;

import com.spaghetti.core.*;
import com.spaghetti.objects.*;
import com.spaghetti.render.*;

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