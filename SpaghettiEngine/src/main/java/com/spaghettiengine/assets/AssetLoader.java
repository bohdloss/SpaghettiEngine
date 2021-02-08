package com.spaghettiengine.assets;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import org.joml.Vector2f;
import org.joml.Vector3f;

import com.spaghettiengine.render.*;
import com.spaghettiengine.utils.ResourceLoader;
import com.spaghettiengine.utils.Utils;

public final class AssetLoader {

	private AssetLoader() {
	}

	// Model loaders

	public static void loadModel(Model model, SheetEntry data) throws Throwable {
		if (data.location().endsWith(".obj")) {
			loadObj(model, data);
			return;
		}
	}

	private static void loadObj(Model model, SheetEntry data) throws Throwable {

		// Load all data

		String source = ResourceLoader.loadText(data.location == null ? data.args[0] : data.location);
		String[] lines = source.split("\\R");
		ArrayList<Vector3f> vertices = new ArrayList<>();
		ArrayList<Vector2f> tex_coords = new ArrayList<>();
		ArrayList<Vector3f> normals = new ArrayList<>();
		/*
		 * Get a face from the list, get a certain vertex index from it, and from it get
		 * the vertex index [0] and the texture index[0]
		 */
		ArrayList<Integer[][]> faces = new ArrayList<>();

		for (String line : lines) {
			String[] tokens = line.split(" ");
			String type = tokens[0];

			switch (type) {
			case "v":

				float vx = Float.parseFloat(tokens[1]);
				float vy = Float.parseFloat(tokens[2]);
				float vz = Float.parseFloat(tokens[3]);

				vertices.add(new Vector3f(vx, vy, vz));

				break;
			case "vt":

				// -x+1 the texture coordinates to match how OpenGL works

				float tx = Float.parseFloat(tokens[1]);
				float ty = -Float.parseFloat(tokens[2]) + 1;

				tex_coords.add(new Vector2f(tx, ty));

				break;
			case "vn":

				float nx = Float.parseFloat(tokens[1]);
				float ny = Float.parseFloat(tokens[2]);
				float nz = Float.parseFloat(tokens[3]);

				normals.add(new Vector3f(nx, ny, nz));

				break;
			case "f":

				Integer[][] facedata = new Integer[tokens.length - 1][0];
				faces.add(facedata);

				for (int j = 0; j < facedata.length; j++) {

					String[] indices = tokens[j + 1].split("/");

					int iv = Integer.parseInt(indices[0]);
					int it = Integer.parseInt(indices[1]);
					int in = Integer.parseInt(indices[2]);

					// In .obj arrays start at 1, subtract it

					facedata[j] = new Integer[3];
					facedata[j][0] = iv - 1;
					facedata[j][1] = it - 1;
					facedata[j][2] = in - 1;

				}

			}
		}

		// Split 4-vertex faces into two 3-vertex faces

		// TODO Use triangulate modifier in your model editor to workaround this

		// Convert faces to OpenGL indices

		ArrayList<Integer> indices = new ArrayList<>();
		ArrayList<Vector3f> _vertices = new ArrayList<>();
		ArrayList<Vector2f> _tex_coords = new ArrayList<>();
		ArrayList<Vector3f> _normals = new ArrayList<>();

		// Loop through faces

		for (Integer[][] facedata : faces) {

			// Loop through face indices

			for (Integer[] element : facedata) {

				// Get each vertex and texture index

				int vindex = element[0];
				int tindex = element[1];
				int nindex = element[2];

				// Obtain info through them

				// This is a unique triplet
				Vector3f vertex = vertices.get(vindex);
				Vector2f tex_coordinate = tex_coords.get(tindex);
				Vector3f normal = normals.get(nindex);

				// Check if this triplet already exists in our OpenGL lists

				// If it is found the index is changed to the appropriate index
				// Otherwise if it is still -1, then it is changed to the current
				// Max index + 1
				int indexFound = -1;

				for (int k = 0; k < _vertices.size(); k++) {

					Vector3f vcompare = _vertices.get(k);
					Vector2f tcompare = _tex_coords.get(k);
					Vector3f ncompare = _normals.get(k);

					boolean vequal = vcompare.x == vertex.x && vcompare.y == vertex.y && vcompare.z == vertex.z;
					boolean tequal = tcompare.x == tex_coordinate.x && tcompare.y == tex_coordinate.y;
					boolean nequal = ncompare.x == normal.x && ncompare.y == normal.y && ncompare.z == normal.z;

					if (vequal && tequal && nequal) {
						indexFound = k;
					}

				}

				if (indexFound == -1) {

					indexFound = _vertices.size();
					// We have a new vertex and texture coord couple
					// We must add it to the list
					_vertices.add(vertex);
					_tex_coords.add(tex_coordinate);
					_normals.add(normal);

				}

				// we add as index the found one whatever it is

				indices.add(indexFound);

			}

		}

		// Convert lists to native arrays

		float[] __vertices = new float[_vertices.size() * 3];
		float[] __tex_coords = new float[_tex_coords.size() * 2];
		float[] __normals = new float[_normals.size() * 3];
		int[] __indices = new int[indices.size()];

		for (int i = 0; i < _vertices.size(); i++) {

			int i3 = i * 3;
			Vector3f vertex = _vertices.get(i);
			__vertices[i3] = vertex.x;
			__vertices[i3 + 1] = vertex.y;
			__vertices[i3 + 2] = vertex.z;

		}

		for (int i = 0; i < _tex_coords.size(); i++) {

			int i2 = i * 2;
			Vector2f tex_coordinates = _tex_coords.get(i);
			__tex_coords[i2] = tex_coordinates.x;
			__tex_coords[i2 + 1] = tex_coordinates.y;

		}

		for (int i = 0; i < _normals.size(); i++) {

			int i3 = i * 3;
			Vector3f normal = _normals.get(i);
			__normals[i3] = normal.x;
			__normals[i3 + 1] = normal.y;
			__normals[i3 + 2] = normal.z;

		}

		for (int i = 0; i < indices.size(); i++) {

			int index = indices.get(i);
			__indices[i] = index;

		}

		// Apply data to Model

		model.setData(new Object[] {__vertices, __tex_coords, __normals, __indices});

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
		shaderProgram.setData((Object[]) shaders);
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

	// Custom loaders

	public static void loadCustom(Asset ro, SheetEntry data) throws Throwable {
		Object[] strings = new Object[data.args.length];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = data.args[i];
		}
		ro.setData(strings);
	}

}
