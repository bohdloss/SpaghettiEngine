package com.spaghettiengine.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.joml.Matrix2f;
import org.joml.Matrix3d;
import org.joml.Matrix3f;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector2d;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;

import com.spaghettiengine.core.Game;

public class Material extends RenderObject {

	public static Material get(String name) {
		return Game.getGame().getAssetManager().material(name);
	}

	public static Material require(String name) {
		return Game.getGame().getAssetManager().requireMaterial(name);
	}

	/*
	 * This class is not abstract because it has enough functionality to work by
	 * itself, but inheritance is encouraged
	 */

	protected ShaderProgram shader;
	protected Texture texture;

	// TODO add default render types

	public Material() {

	}

	public Material(Texture texture) {
		setTexture(texture);
	}

	public Material(Texture texture, ShaderProgram shader) {
		setTexture(texture);
		setShader(shader);
		load();
	}

	public Material(ShaderProgram shader) {
		setShader(shader);
	}

	public void setData(Texture texture, ShaderProgram shader) {
		if (valid()) {
			return;
		}

		this.texture = texture;
		this.shader = shader;

		setFilled(true);
	}

	// Getters and setters

	public ShaderProgram getShader() {
		return shader;
	}

	public Texture getTexture() {
		return texture;
	}

	// TIP: Setters can be overridden to do nothing at will

	public void setShader(ShaderProgram shader) {
		if (shader == null) {
			throw new IllegalArgumentException();
		}
		this.shader = shader;
	}

	public void setTexture(Texture texture) {
		if (texture == null) {
			throw new IllegalArgumentException();
		}
		this.texture = texture;
	}

	// Use method

	public void use() {
		if (!valid()) {
			return;
		}

		shader.use();
		texture.use(0);

		setDefaultProperties();
	}

	public void setDefaultProperties() {
		// TODO
	}

	// Port of all the uniform functions

	// Float, float array, float buffer

	public void setProperty(String name, float property) {
		if (!valid()) {
			return;
		}
		shader.setFloatUniform(name, property);
	}

	public void setProperty(String name, float[] property) {
		if (!valid()) {
			return;
		}
		shader.setFloatArrayUniform(name, property);
	}

	public void setProperty(String name, FloatBuffer property) {
		if (!valid()) {
			return;
		}
		shader.setFloatBufferUniform(name, property);
	}

	// Int, int array, int buffer

	public void setProperty(String name, int property) {
		if (!valid()) {
			return;
		}
		shader.setIntUniform(name, property);
	}

	public void setProperty(String name, int[] property) {
		if (!valid()) {
			return;
		}
		shader.setIntArrayUniform(name, property);
	}

	public void setProperty(String name, IntBuffer property) {
		if (!valid()) {
			return;
		}
		shader.setIntBufferUniform(name, property);
	}

	// Vector 2

	public void setProperty(String name, float x, float y) {
		if (!valid()) {
			return;
		}
		shader.setVec2Uniform(name, x, y);
	}

	public void setProperty(String name, Vector2f property) {
		if (!valid()) {
			return;
		}
		shader.setVec2Uniform(name, property);
	}

	public void setProperty(String name, double x, double y) {
		if (!valid()) {
			return;
		}
		shader.setVec2Uniform(name, x, y);
	}

	public void setProperty(String name, Vector2d property) {
		if (!valid()) {
			return;
		}
		shader.setVec2Uniform(name, property);
	}

	// Vector 3

	public void setProperty(String name, float x, float y, float z) {
		if (!valid()) {
			return;
		}
		shader.setVec3Uniform(name, x, y, z);
	}

	public void setProperty(String name, Vector3f property) {
		if (!valid()) {
			return;
		}
		shader.setVec3Uniform(name, property);
	}

	public void setProperty(String name, double x, double y, double z) {
		if (!valid()) {
			return;
		}
		shader.setVec3Uniform(name, x, y, z);
	}

	public void setProperty(String name, Vector3d property) {
		if (!valid()) {
			return;
		}
		shader.setVec3Uniform(name, property);
	}

	// Vector 4

	public void setProperty(String name, float x, float y, float z, float w) {
		if (!valid()) {
			return;
		}
		shader.setVec4Uniform(name, x, y, z, w);
	}

	public void setProperty(String name, Vector4f property) {
		if (!valid()) {
			return;
		}
		shader.setVec4Uniform(name, property);
	}

	public void setProperty(String name, double x, double y, float z, float w) {
		if (!valid()) {
			return;
		}
		shader.setVec4Uniform(name, x, y, z, w);
	}

	public void setProperty(String name, Vector4d property) {
		if (!valid()) {
			return;
		}
		shader.setVec4Uniform(name, property);
	}

	// Matrix 2, 3, 4

	public void setProperty(String name, Matrix2f property) {
		if (!valid()) {
			return;
		}
		shader.setMat2Uniform(name, property);
	}

	public void setProperty(String name, Matrix3f property) {
		if (!valid()) {
			return;
		}
		shader.setMat3Uniform(name, property);
	}

	public void setProperty(String name, Matrix3d property) {
		if (!valid()) {
			return;
		}
		shader.setMat3Uniform(name, property);
	}

	public void setProperty(String name, Matrix4f property) {
		if (!valid()) {
			return;
		}
		shader.setMat4Uniform(name, property);
	}

	public void setProperty(String name, Matrix4d property) {
		if (!valid()) {
			return;
		}
		shader.setMat4Uniform(name, property);
	}

	// Set projection

	public void setProjection(Matrix4f projection) {
		if (!valid()) {
			return;
		}
		shader.setProjection(projection);
	}

	public void setProjection(Matrix4d projection) {
		if (!valid()) {
			return;
		}
		shader.setProjection(projection);
	}

	// Useless

	@Override
	protected void load0() {
	}

	@Override
	protected void delete0() {
	}

	@Override
	protected void reset0() {
	}

}