package com.spaghetti.physics.d2;

import org.joml.Vector2f;

import com.spaghetti.physics.RaycastRequest;

public class RaycastRequest2D extends RaycastRequest<Vector2f, Float, RigidBody2D> {

	public RaycastRequest2D() {
		beginning = new Vector2f();
		end = new Vector2f();
	}

}
