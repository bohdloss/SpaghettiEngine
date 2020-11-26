package com.spaghettiengine.components;

import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;

public class Mesh extends GameComponent {

	protected Model model;
	protected Material material;

	public Mesh(Level level, GameComponent parent, Model model, Material material) {
		super(level, parent);
		this.model = model;
		this.material = material;
	}

	public Mesh(Level level, GameComponent parent) {
		this(level, parent, (Model)null, (Material)null);
	}
	
	@Override
	public void renderUpdate() {
		material.use();
		material.setProjection(cache);
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
