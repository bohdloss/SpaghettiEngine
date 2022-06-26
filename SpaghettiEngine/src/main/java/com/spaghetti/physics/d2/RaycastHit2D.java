package com.spaghetti.physics.d2;

import org.joml.Vector2f;

import com.spaghetti.physics.RaycastHit;

public class RaycastHit2D extends RaycastHit<Vector2f, Float, RigidBody2D> {

	public RaycastHit2D() {
		point = new Vector2f();
		normal = new Float(0);
	}

}
