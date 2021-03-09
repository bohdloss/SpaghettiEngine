package com.spaghetti.objects;

import org.joml.Matrix4d;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.Replicate;
import com.spaghetti.render.*;

public class Mesh extends GameObject {

	@Replicate
	protected Model model;
	@Replicate
	protected Material material;

	@Replicate
	private int a;

	public Mesh(Level level, GameObject parent, Model model, Material material) {
		super(level, parent);
		this.model = model;
		this.material = material;
	}

	public Mesh(Level level, GameObject parent) {
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

}