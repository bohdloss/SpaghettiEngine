package com.spaghetti.demo;

import org.joml.Vector3d;

import com.spaghetti.core.*;
import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.objects.*;
import com.spaghetti.render.*;

@Bidirectional
public class Player extends GameObject {

	public Player() {
		if(Game.getGame().hasAuthority()) {
			addChild(new Mesh(Model.get("square"), Material.get("defaultMAT")));
			addChild(new Camera());
			getChild(Camera.class).setFov(20);
			addComponent(new PlayerController());
		}
	}

	@Override
	protected void onBeginPlay() {
		if (!getGame().isMultiplayer()) {
			getLevel().attachCamera(getChild(Camera.class));
			getLevel().attachController(getComponent(Controller.class));
		}
	}

	public boolean isLocal() {
		return getGame().getClient() == null ? true : getGame().getClient().getWorker().player == this;
	}
	
	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		if(buffer.getWorker().player != this) {
			Vector3d vec = new Vector3d();
			getRelativePosition(vec);
			buffer.putDouble(vec.x);
			buffer.putDouble(vec.y);
			buffer.putDouble(vec.z);
		}
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
		if(buffer.getWorker().player == this) {
			Vector3d vec = new Vector3d();
			vec.x = buffer.getDouble();
			vec.y = buffer.getDouble();
			vec.z = buffer.getDouble();
			setRelativePosition(vec);
		}
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
		if(isLocal()) {
			Vector3d vec = new Vector3d();
			getRelativePosition(vec);
			buffer.putDouble(vec.x);
			buffer.putDouble(vec.y);
			buffer.putDouble(vec.z);
		}
	}
	
	@Override
	public void readDataClient(NetworkBuffer buffer) {
		if(!isLocal()) {
			Vector3d vec = new Vector3d();
			vec.x = buffer.getDouble();
			vec.y = buffer.getDouble();
			vec.z = buffer.getDouble();
			setRelativePosition(vec);
		}
	}
	
}