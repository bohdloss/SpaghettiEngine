package com.spaghettiengine.interfaces;

import org.joml.Matrix4d;

public interface Renderable {

	public abstract void render(Matrix4d projection, double delta);

}
