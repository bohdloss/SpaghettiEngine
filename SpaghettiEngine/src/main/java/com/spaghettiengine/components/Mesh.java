package com.spaghettiengine.components;

import org.joml.Matrix4d;

import com.spaghettiengine.core.*;
import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.SpaghettiBuffer;

public class Mesh extends GameComponent {

	protected Model model;
	protected Material material;

	public Mesh(Level level, GameComponent parent, Model model, Material material) {
		super(level, parent);
		this.model = model;
		this.material = material;
	}

	public Mesh(Level level, GameComponent parent) {
		this(level, parent, (Model) null, (Material) null);
	}

	@Override
	public void render(Matrix4d projection, double delta) {
		material.use();
		material.setProjection(projection);
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

	// Interface

	@Override
	public void getReplicateData(SpaghettiBuffer buffer) {

		// TODO when asset manager is implemented

	}

	@Override
	public void setReplicateData(SpaghettiBuffer buffer) {

		// TODO - - - - -

	}

	@Override
	public void serverUpdate(double delta) {
	}

	@Override
	public void clientUpdate(double delta) {
	}

}
