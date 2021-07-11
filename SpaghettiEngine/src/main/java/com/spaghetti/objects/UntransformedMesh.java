package com.spaghetti.objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.spaghetti.interfaces.ToClient;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

@ToClient
public class UntransformedMesh extends Mesh {

	public UntransformedMesh(Model model, Material material) {
		this.model = model;
		this.material = material;
	}

	public UntransformedMesh() {
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
			Matrix4f matrix = renderer.getUntransformedProjection();
			matrix.translate(position);
			matrix.rotateXYZ(rotation);
			matrix.scale(scale);

			// Render
			material.use();
			material.setProjection(matrix);
			model.render();
		}
	}

}
