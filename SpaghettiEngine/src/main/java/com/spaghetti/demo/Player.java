package com.spaghetti.demo;

import org.joml.Vector3d;

import com.spaghetti.core.*;
import com.spaghetti.input.Controller;
import com.spaghetti.networking.NetworkBuffer;
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
		if(!getGame().isMultiplayer()) {
			getLevel().attachCamera(getChild(Camera.class));
			getLevel().attachController(getComponent(Controller.class));
		}
	}

	@Override
	public void writeData(boolean isClient, NetworkBuffer buffer) {
//		super.writeData(isClient, buffer);
		if(isClient) {
			Vector3d vec = new Vector3d();
			getRelativePosition(vec);
			buffer.putDouble(vec.x);
			buffer.putDouble(vec.y);
			buffer.putDouble(vec.z);
		}
	}
	
	@Override
	public void readData(boolean isClient, NetworkBuffer buffer) {
//		super.readData(isClient, buffer);
		if(!isClient) {
			Vector3d vec = new Vector3d();
			vec.x = buffer.getDouble();
			vec.y = buffer.getDouble();
			vec.z = buffer.getDouble();
			setRelativePosition(vec);
		}
	}
	
}