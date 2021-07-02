package com.spaghetti.objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.spaghetti.core.*;
import com.spaghetti.interfaces.ToClient;
import com.spaghetti.networking.NetworkBuffer;
import com.spaghetti.render.*;

@ToClient
public class Mesh extends GameObject {

	protected Model model;
	protected Material material;

	public Mesh(Model model, Material material) {
		this.model = model;
		this.material = material;
	}

	public Mesh() {
		this((Model) null, (Material) null);
	}

	@Override
	public void render(Camera renderer, float delta) {
		if (material != null && model != null) {

			// Gather transform data
			Vector3f position = new Vector3f();
			Vector3f rotation = new Vector3f();
			Vector3f scale = new Vector3f();
			getWorldPosition(position);
			getWorldRotation(rotation);
			getWorldScale(scale);

			// Transform matrix
			Matrix4f matrix = renderer.getProjection();
			matrix.translate(position);
			matrix.rotateXYZ(rotation);
			matrix.scale(scale);

			// Render
			material.use();
			material.setProjection(matrix);
			model.render();
		}
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

	@Override
	public void writeDataServer(NetworkBuffer buffer) {
		System.out.println("b");
		super.writeDataServer(buffer);
		buffer.putString(true, model == null ? "" : model.getName(), NetworkBuffer.UTF_8);
		buffer.putString(true, material == null ? "" : material.getName(), NetworkBuffer.UTF_8);
	}

	@Override
	public void readDataClient(NetworkBuffer buffer) {
		System.out.println("a");
		super.readDataClient(buffer);
		String modelname = buffer.getString(true, NetworkBuffer.UTF_8);
		String materialname = buffer.getString(true, NetworkBuffer.UTF_8);
		this.model = modelname.equals("") ? null : getGame().getAssetManager().model(modelname);
		this.material = materialname.equals("") ? null : getGame().getAssetManager().material(materialname);
	}

}