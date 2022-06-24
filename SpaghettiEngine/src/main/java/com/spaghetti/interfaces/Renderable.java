package com.spaghetti.interfaces;

import org.joml.Vector3f;

import com.spaghetti.objects.Camera;
import com.spaghetti.utils.Transform;

public interface Renderable {

	public abstract void render(Camera renderer, float delta, Transform transform);

}
