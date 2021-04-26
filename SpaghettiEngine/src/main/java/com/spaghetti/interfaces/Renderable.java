package com.spaghetti.interfaces;

import org.joml.Matrix4f;

public interface Renderable {

	public abstract void render(Matrix4f projection, float delta);

}
