package com.spaghetti.objects;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

public class SkyboxMesh extends Mesh {

	public SkyboxMesh(Model model, Material material) {
		this.model = model;
		this.material = material;
	}

	public SkyboxMesh() {
		this((Model) null, (Material) null);
	}

	@Override
	public void render(Matrix4d projection, double delta) {
		if (material != null && model != null) {

			// Simulate camera rendering

			Matrix4d sceneMatrix = new Matrix4d();
			Vector3d vec3cache = new Vector3d();

			// Reset matrix
			sceneMatrix.set(getLevel().getActiveCamera().getUntransformedProjection());

			// Get world position
			getWorldPosition(vec3cache);
			sceneMatrix.translate(vec3cache);

			// Get world rotation
			getWorldRotation(vec3cache);
			sceneMatrix.rotateXYZ(vec3cache);

			// Get world scale
			getWorldScale(vec3cache);
			sceneMatrix.scale(vec3cache.x, vec3cache.y, 1);

			material.use();
			material.setProjection(sceneMatrix);
			model.render();
		}
	}

}
