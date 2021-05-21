package com.spaghetti.demo;

import org.joml.Vector3f;

import com.spaghetti.core.*;
import com.spaghetti.input.Controller;
import com.spaghetti.interfaces.*;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.objects.*;
import com.spaghetti.render.*;

@Bidirectional
public class Player extends GameObject {

	public Player() {
		if (Game.getGame().hasAuthority()) {
			addChild(new Mesh(Model.get("apple_model"), Material.get("apple_mat")));
			addChild(new Camera());
			getChild(Camera.class).setFov(20);
			addComponent(new PlayerController());
		}
	}

	@Override
	protected void onBeginPlay() {
		if (!getGame().isMultiplayer()) {
			getGame().attachCamera(getChild(Camera.class));
			getGame().attachController(getComponent(Controller.class));
		}
	}

	public boolean isLocal() {
		return getGame().getClient() == null ? true : getGame().getClient().getWorker().player == this;
	}

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		if (buffer.getWorker().player != this) {
			Vector3f vec = new Vector3f();
			getRelativePosition(vec);
			buffer.putFloat(vec.x);
			buffer.putFloat(vec.y);
			buffer.putFloat(vec.z);
		}
	}

	@Override
	public void readDataServer(NetworkBuffer buffer) {
		if (buffer.getWorker().player == this) {
			Vector3f vec = new Vector3f();
			vec.x = buffer.getFloat();
			vec.y = buffer.getFloat();
			vec.z = buffer.getFloat();
			setRelativePosition(vec);
		}
	}

	@Override
	public void writeDataClient(NetworkBuffer buffer) {
		if (isLocal()) {
			Vector3f vec = new Vector3f();
			getRelativePosition(vec);
			buffer.putFloat(vec.x);
			buffer.putFloat(vec.y);
			buffer.putFloat(vec.z);
		}
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		if (!isLocal()) {
			Vector3f vec = new Vector3f();
			vec.x = buffer.getFloat();
			vec.y = buffer.getFloat();
			vec.z = buffer.getFloat();
			setRelativePosition(vec);
		}
	}

	@Override
	public boolean getReplicateFlag() {
		return true;
	}

}