package com.spaghetti.objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;
import com.spaghetti.utils.Transform;

public class UntransformedMesh extends Mesh {

	public UntransformedMesh(Model model, Material material) {
		super(model, material);
	}

	public UntransformedMesh() {
		super((Model) null, (Material) null);
	}

	@Override
	public void render(Camera renderer, float delta, Transform transform) {
		if (material != null && model != null) {

			// Gather transform data

			// Transform matrix
			Matrix4f matrix = renderer.getUntransformedProjection();
			matrix.translate(transform.position);
			matrix.rotateXYZ(transform.rotation);
			matrix.scale(transform.scale);

			// Render
			material.use();
			material.setProjection(matrix);
			model.render();
		}
	}

}
