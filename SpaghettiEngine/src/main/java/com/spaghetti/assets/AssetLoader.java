package com.spaghetti.assets;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;
import org.lwjgl.openal.AL10;
import com.spaghetti.audio.SoundBuffer;
import com.spaghetti.render.*;
import com.spaghetti.utils.ResourceLoader;
import com.spaghetti.utils.Utils;

public final class AssetLoader {

	private AssetLoader() {
	}

	// Model loaders

	public static void loadModel(Model model, SheetEntry data) throws Throwable {
		// Load raw data into direct byte buffer
		InputStream data_stream = ResourceLoader.getStream(data.location());
		int available = data_stream.available();
		ByteBuffer data_buffer = BufferUtils.createByteBuffer(available);
		ResourceLoader.loadBinaryToBuffer(data_stream, data_buffer);
		data_buffer.clear();

//		// Import
		AIScene scene = Assimp.aiImportFileEx(data.location(),
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
		if (data.args[0].equals("find")) {
			boolean found = false;

			for (AIMesh mesh : meshes) {
				if (Utils.getAssimpString(mesh.mName()).equals(data.args[1])) {
					// We found the mesh we care about, parse it
					found = true;
					internal_parsemesh(mesh, _indices, _vertices, _tex_coords, _normals);
					break;
				} // End of name condition
			} // End of meshes loop
			if (!found) {
				throw new IllegalArgumentException("Couldn't find mesh " + data.args[1]);
			}
		} else if (data.args[0].equals("unify")) {
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

		// Apply data to Model
		model.setData(new Object[] { __vertices, __tex_coords, __normals, __indices });

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

		for (int j = 0; j < num_vertices; j++) {
			_vertices.add(mesh.mVertices().get(j));
		}

		for (int j = 0; j < num_textures; j++) {
			_tex_coords.add(mesh.mTextureCoords(0).get(j));
		}

		for (int j = 0; j < num_normals; j++) {
			_normals.add(mesh.mNormals().get(j));
		}

		for (int i = 0; i < num_faces; i++) {
			_indices.add(mesh.mFaces().get(i).mIndices().get(0) + _vertices_size);
			_indices.add(mesh.mFaces().get(i).mIndices().get(1) + _tex_coords_size);
			_indices.add(mesh.mFaces().get(i).mIndices().get(2) + _normals_size);
		}
	}

	// Shader loaders

	public static void loadShader(Shader shader, SheetEntry data) throws Throwable {
		String location = data.location();
		String source = ResourceLoader.loadText(location);
		shader.setData(source, Integer.parseInt(data.args[0]));
	}

	// Shader program loaders

	public static void loadShaderProgram(AssetManager manager, ShaderProgram shaderProgram, SheetEntry data)
			throws Throwable {
		Object[] shaders = new Object[data.args.length];
		for (int i = 0; i < data.args.length; i++) {
			shaders[i] = manager.requireShader(data.args[i]);
		}
		shaderProgram.setData(shaders);
	}

	// Texture loaders

	public static void loadTexture(Texture texture, SheetEntry data) throws Throwable {
		BufferedImage img = ResourceLoader.loadImage(data.location());
		texture.setData(Utils.parseImage(img), img.getWidth(), img.getHeight(), Texture.COLOR, Texture.NEAREST);
	}

	// Material loaders

	public static void loadMaterial(AssetManager manager, Material material, SheetEntry data) throws Throwable {
		material.setData(manager.requireTexture(data.args[0]), manager.requireShaderProgram(data.args[1]));
	}

	// Sound loaders

	public static void loadSoundBuffer(SoundBuffer buffer, SheetEntry data) throws Throwable {
		AudioInputStream audio_stream = AudioSystem.getAudioInputStream(ResourceLoader.getStream(data.location()));
		AudioFormat audio_format = audio_stream.getFormat();

		// Read metadata
		int channels = audio_format.getChannels();
		int bps = audio_format.getSampleSizeInBits();
		int samplerate = (int) audio_format.getSampleRate();

		int format = 0;
		if (channels == 1) {
			if (bps == 8) {
				format = AL10.AL_FORMAT_MONO8;
			} else if (bps == 16) {
				format = AL10.AL_FORMAT_MONO16;
			}
		} else if (channels == 2) {
			if (bps == 8) {
				format = AL10.AL_FORMAT_STEREO8;
			} else if (bps == 16) {
				format = AL10.AL_FORMAT_STEREO16;
			}
		}

		// Read audio data
		byte[] audio_data = new byte[audio_stream.available()];
		Utils.effectiveRead(audio_stream, audio_data, 0, audio_data.length);
		ByteBuffer raw_data = ByteBuffer.wrap(audio_data);
		raw_data.order(ByteOrder.LITTLE_ENDIAN);
		ByteBuffer dest_data = BufferUtils.createByteBuffer(audio_data.length);
		dest_data.order(ByteOrder.nativeOrder());

		// Reorder bytes
		int bytes = bps / 8;
		while (raw_data.hasRemaining()) {
			int pos = raw_data.position();
			for (int i = 0; i < bytes; i++) {
				dest_data.put(
						dest_data.order() == raw_data.order() ? raw_data.get(pos + i) : raw_data.get(pos + bytes - i));
			}
			raw_data.position(pos + bytes);
		}
		dest_data.clear();

		buffer.setData(format, dest_data, samplerate);
	}

	// Custom loaders

	public static void loadCustom(Asset ro, SheetEntry data) throws Throwable {
		Object[] strings = new Object[data.args.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = data.args[i];
		}
		ro.setData(strings);
	}

}
