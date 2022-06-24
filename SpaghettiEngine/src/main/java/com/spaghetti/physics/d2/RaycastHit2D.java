package com.spaghetti.physics.d2;

import com.spaghetti.physics.RaycastHit;
import org.joml.Vector2f;

public class RaycastHit2D extends RaycastHit<Vector2f, Float, RigidBody2D> {

	public RaycastHit2D() {
		point = new Vector2f();
		normal = new Float(0);
	}

}
