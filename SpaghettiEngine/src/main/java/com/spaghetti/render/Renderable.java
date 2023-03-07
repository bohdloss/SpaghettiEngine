package com.spaghetti.render;

import com.spaghetti.utils.Transform;

public interface Renderable {

	public abstract void render(Camera renderer, float delta, Transform transform);

}
