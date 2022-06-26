package com.spaghetti.assets;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AICamera;
import org.lwjgl.assimp.AILight;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.Assimp;

import com.spaghetti.interfaces.AssetLoader;
import com.spaghetti.render.Model;
import com.spaghetti.utils.ResourceLoader;
import com.spaghetti.utils.Utils;

public class ModelLoader implements AssetLoader {

	@Override
	public void initializeAsset(SheetEntry asset) {
		asset.asset = new Model();
	}

	@Override
	public String[] provideDependencies(SheetEntry asset) {
		return null;

	}

	@Override
	public Object[] loadAsset(SheetEntry asset) throws Throwable {
		// Load raw data into direct byte buffer
		InputStream data_stream = ResourceLoader.getStream(asset.args[0]);
		int available = data_stream.available();
		ByteBuffer data_buffer = BufferUtils.createByteBuffer(available);
		ResourceLoader.loadBinaryToBuffer(data_stream, data_buffer);
		data_buffer.clear();

		// Import
		AIScene scene = Assimp.aiImportFileEx(asset.args[0],
				Assimp.aiProcess_JoinIdenticalVertices | Assimp.aiProcess_Triangulate | Assimp.aiProcess_GenUVCoords
						| Assimp.aiProcess_TransformUVCoords | Assimp.aiProcess_FindDegenerates
						| Assimp.aiProcess_FindInvalidData | Assimp.aiProcess_FixInfacingNormals
						| Assimp.aiProcess_OptimizeMeshes | Assimp.aiProcess_OptimizeGraph | Assimp.aiProcess_FlipUVs,
				JarFileIO.getInstance());
		Utils.aiError();

		// Allocate every object needed
		AIMesh[] meshes = new AIMesh[scene.mNumMeshes()];
		for (int i = 0; i < meshes.length; i++) {
			meshes[i] = AIMesh.createSafe(scene.mMeshes().get(i));
		}

		AIAnimation[] animations = new AIAnimation[scene.mNumAnimations()];
		for (int i = 0; i < animations.length; i++) {
			animations[i] = AIAnimation.createSafe(scene.mAnimations().get(i));
		}

		AIMaterial[] materials = new AIMaterial[scene.mNumMaterials()];
		for (int i = 0; i < materials.length; i++) {
			materials[i] = AIMaterial.createSafe(scene.mMaterials().get(i));
		}

		AICamera[] cameras = new AICamera[scene.mNumCameras()];
		for (int i = 0; i < cameras.length; i++) {
			cameras[i] = AICamera.createSafe(scene.mCameras().get(i));
		}

		AITexture[] textures = new AITexture[scene.mNumTextures()];
		for (int i = 0; i < textures.length; i++) {
			textures[i] = AITexture.createSafe(scene.mTextures().get(i));
		}

		AILight[] lights = new AILight[scene.mNumLights()];
		for (int i = 0; i < lights.length; i++) {
			lights[i] = AILight.createSafe(scene.mLights().get(i));
		}

		// Mesh buffers
		ArrayList<Integer> _indices = new ArrayList<>();
		ArrayList<AIVector3D> _vertices = new ArrayList<>();
		ArrayList<AIVector3D> _tex_coords = new ArrayList<>();
		ArrayList<AIVector3D> _normals = new ArrayList<>();

		// Iterate over meshes
		if (asset.args[1].equals("find")) {
			boolean found = false;

			for (AIMesh mesh : meshes) {
				if (Utils.getAssimpString(mesh.mName()).equals(asset.args[2])) {
					// We found the mesh we care about, parse it
					found = true;
					internal_parsemesh(mesh, _indices, _vertices, _tex_coords, _normals);
					break;
				} // End of name condition
			} // End of meshes loop
			if (!found) {
				throw new IllegalArgumentException("Couldn't find mesh " + asset.args[2]);
			}
		} else if (asset.args[1].equals("unify")) {
			for (AIMesh element : meshes) {
				internal_parsemesh(element, _indices, _vertices, _tex_coords, _normals);
			}
		}

		// Conversion to native arrays
		float[] __vertices = new float[_vertices.size() * 3];
		float[] __tex_coords = new float[_tex_coords.size() * 2];
		float[] __normals = new float[_normals.size() * 3];
		int[] __indices = new int[_indices.size()];

		for (int i = 0; i < _vertices.size(); i++) {

			int i3 = i * 3;
			AIVector3D vertex = _vertices.get(i);
			__vertices[i3] = vertex.x();
			__vertices[i3 + 1] = vertex.y();
			__vertices[i3 + 2] = vertex.z();

		}

		for (int i = 0; i < _tex_coords.size(); i++) {

			int i2 = i * 2;
			AIVector3D tex_coordinates = _tex_coords.get(i);
			__tex_coords[i2] = tex_coordinates.x();
			__tex_coords[i2 + 1] = tex_coordinates.y();

		}

		for (int i = 0; i < _normals.size(); i++) {

			int i3 = i * 3;
			AIVector3D normal = _normals.get(i);
			__normals[i3] = normal.x();
			__normals[i3 + 1] = normal.y();
			__normals[i3 + 2] = normal.z();

		}

		for (int i = 0; i < _indices.size(); i++) {

			int index = _indices.get(i);
			__indices[i] = index;

		}
		return new Object[] { __vertices, __tex_coords, __normals, __indices };
	}

	private static void internal_parsemesh(AIMesh mesh, ArrayList<Integer> _indices, ArrayList<AIVector3D> _vertices,
			ArrayList<AIVector3D> _tex_coords, ArrayList<AIVector3D> _normals) {
		// Allocate resources

		int _vertices_size = _vertices.size();
		int _tex_coords_size = _tex_coords.size();
		int _normals_size = _normals.size();

		int num_vertices = mesh.mNumVertices();
		int num_textures = (mesh.mTextureCoords() == null || mesh.mTextureCoords().limit() == 0
				|| mesh.mTextureCoords(0) == null) ? 0 : mesh.mTextureCoords(0).limit();
		int num_normals = (mesh.mNormals() == null) ? 0 : mesh.mNormals().limit();

		int num_faces = mesh.mNumFaces();

		// Add to lists
		for (int j = 0; j < num_vertices; j++) {
			_vertices.add(mesh.mVertices().get(j));
		}

		for (int j = 0; j < num_textures; j++) {
			_tex_coords.add(mesh.mTextureCoords(0).get(j));
		}

		for (int j = 0; j < num_normals; j++) {
			_normals.add(mesh.mNormals().get(j));
		}

		// Add indices
		for (int i = 0; i < num_faces; i++) {
			_indices.add(mesh.mFaces().get(i).mIndices().get(0) + _vertices_size);
			_indices.add(mesh.mFaces().get(i).mIndices().get(1) + _tex_coords_size);
			_indices.add(mesh.mFaces().get(i).mIndices().get(2) + _normals_size);
		}
	}

	@Override
	public Object[] provideDefault(SheetEntry asset) {
		// TODO Auto-generated method stub
		return null;
	}

}
