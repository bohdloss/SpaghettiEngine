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

public class Material {

	/*
	 * This class is not abstract because it has enough functionality to work by
	 * itself, but inheritance is encouraged
	 */

	protected ShaderProgram shader = ShaderProgram.DEFAULT_SHADER;
	protected Texture texture;

	public Material(Texture texture) {
		if (texture == null) {
			throw new IllegalArgumentException();
		}
		this.texture = texture;
	}

	public Material(Texture texture, ShaderProgram shader) {
		this(texture);
		if (shader == null) {
			throw new IllegalArgumentException();
		}
		this.shader = shader;
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
		shader.setFloatUniform(name, property);
	}

	public void setProperty(String name, float[] property) {
		shader.setFloatArrayUniform(name, property);
	}

	public void setProperty(String name, FloatBuffer property) {
		shader.setFloatBufferUniform(name, property);
	}

	// Int, int array, int buffer

	public void setProperty(String name, int property) {
		shader.setIntUniform(name, property);
	}

	public void setProperty(String name, int[] property) {
		shader.setIntArrayUniform(name, property);
	}

	public void setProperty(String name, IntBuffer property) {
		shader.setIntBufferUniform(name, property);
	}

	// Vector 2

	public void setProperty(String name, float x, float y) {
		shader.setVec2Uniform(name, x, y);
	}

	public void setProperty(String name, Vector2f property) {
		shader.setVec2Uniform(name, property);
	}

	public void setProperty(String name, double x, double y) {
		shader.setVec2Uniform(name, x, y);
	}

	public void setProperty(String name, Vector2d property) {
		shader.setVec2Uniform(name, property);
	}

	// Vector 3

	public void setProperty(String name, float x, float y, float z) {
		shader.setVec3Uniform(name, x, y, z);
	}

	public void setProperty(String name, Vector3f property) {
		shader.setVec3Uniform(name, property);
	}

	public void setProperty(String name, double x, double y, double z) {
		shader.setVec3Uniform(name, x, y, z);
	}

	public void setProperty(String name, Vector3d property) {
		shader.setVec3Uniform(name, property);
	}

	// Vector 4

	public void setProperty(String name, float x, float y, float z, float w) {
		shader.setVec4Uniform(name, x, y, z, w);
	}

	public void setProperty(String name, Vector4f property) {
		shader.setVec4Uniform(name, property);
	}

	public void setProperty(String name, double x, double y, float z, float w) {
		shader.setVec4Uniform(name, x, y, z, w);
	}

	public void setProperty(String name, Vector4d property) {
		shader.setVec4Uniform(name, property);
	}

	// Matrix 2, 3, 4

	public void setProperty(String name, Matrix2f property) {
		shader.setMat2Uniform(name, property);
	}

	public void setProperty(String name, Matrix3f property) {
		shader.setMat3Uniform(name, property);
	}

	public void setProperty(String name, Matrix3d property) {
		shader.setMat3Uniform(name, property);
	}

	public void setProperty(String name, Matrix4f property) {
		shader.setMat4Uniform(name, property);
	}

	public void setProperty(String name, Matrix4d property) {
		shader.setMat4Uniform(name, property);
	}

	// Set projection

	public void setProjection(Matrix4f projection) {
		shader.setProjection(projection);
	}

	public void setProjection(Matrix4d projection) {
		shader.setProjection(projection);
	}

}