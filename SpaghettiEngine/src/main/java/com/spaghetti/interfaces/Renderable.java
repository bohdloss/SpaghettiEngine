package com.spaghetti.interfaces;

import com.spaghetti.objects.Camera;

public interface Renderable {

	public abstract void render(Camera renderer, float delta);

}
