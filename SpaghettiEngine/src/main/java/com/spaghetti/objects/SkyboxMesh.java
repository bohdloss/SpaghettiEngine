package com.spaghetti.objects;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.spaghetti.interfaces.ToClient;
import com.spaghetti.render.Material;
import com.spaghetti.render.Model;

@ToClient
public class SkyboxMesh extends Mesh {

	public SkyboxMesh(Model model, Material material) {
		this.model = model;
		this.material = material;
	}

	public SkyboxMesh() {
		this((Model) null, (Material) null);
	}

	@Override
	public void render(Matrix4f projection, float delta) {
		if (material != null && model != null) {

			// Simulate camera rendering

			Matrix4f sceneMatrix = new Matrix4f();
			Vector3f vec3cache = new Vector3f();

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
