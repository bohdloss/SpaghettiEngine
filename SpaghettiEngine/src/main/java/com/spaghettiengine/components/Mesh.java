package com.spaghettiengine.components;

import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;

public class Mesh extends GameComponent {

	protected Model model;
	protected Material material;

	public Mesh(Level level, GameComponent parent) {
		super(level, parent);
	}

	@Override
	public void renderUpdate() {
		material.use();
		model.render();
	}

	public Model getModel() {
		return model;
	}

	public Material getMaterial() {
		return material;
	}

	public void setModel(Model model) {
		this.model = model;
	}

	public void setMaterial(Material material) {
		this.material = material;
	}

}
