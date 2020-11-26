package com.spaghettiengine.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class ShaderProgram {

	public static final ShaderProgram DEFAULT_SHADER = null; // TODO
	private static final String PROJECTION = "projection";

	protected final int id;
	private boolean deleted;

	// cache
	private FloatBuffer mat4 = BufferUtils.createFloatBuffer(16);

	// keep track of valid uniform locations
	private HashMap<String, Integer> locations = new HashMap<>();

	public ShaderProgram(Shader... shaders) {

		// Get a usable id for this s-program
		this.id = GL20.glCreateProgram();

		try {

			// Link all shaders
			for (Shader shader : shaders) {
				if (shader.isDeleted()) {
					throw new IllegalArgumentException("Deleted shader");
				}
				GL20.glAttachShader(id, shader.getId());
			}

			// Basic attribs
			GL20.glBindAttribLocation(id, 0, "vertices");
			GL20.glBindAttribLocation(id, 1, "textures");

			// Perform linking and check if it worked
			GL20.glLinkProgram(id);

			if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Linker error: " + GL20.glGetProgramInfoLog(id));
			}

			// Perform validation and check if it worked
			GL20.glValidateProgram(id);

			if (GL20.glGetProgrami(id, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Validator error: " + GL20.glGetProgramInfoLog(id));
			}

		} catch (Throwable t) {

			// In case an error occurs, avoid leaking memory
			delete();

			// Pass the throwable back up the stack
			throw t;

		} finally {

			// Whatever happens, shaders should be detached
			for (Shader shader : shaders) {
				GL20.glDetachShader(id, shader.getId());
			}

		}
	}

	public void use() {
		GL20.glUseProgram(id);

	}

	public void delete() {
		if (!deleted) {
			GL20.glDeleteProgram(id);
			deleted = true;
		}
	}

	public boolean isDeleted() {
		return deleted;
	}

	public int getId() {
		return id;
	}

	// These are the most commonly used uniform methods and so are on top

	private int getUniformLocation(String name) {
		if (locations.containsKey(name)) {
			// Check if the location is cached
			return locations.get(name);
		} else {
			// Find the location and cache it
			int loc = GL20.glGetUniformLocation(id, name);
			if (loc == -1) {
				throw new IllegalArgumentException("Invalid uniform name");
			}
			locations.put(name, loc);
			return loc;
		}
	}

	public void setMat4Uniform(String name, Matrix4d value) {
		int loc = getUniformLocation(name);
		value.get(mat4);
		GL20.glUniformMatrix4fv(loc, false, mat4);
	}

	public void setProjection(Matrix4d projection) {
		setMat4Uniform(PROJECTION, projection);
	}

	public void setProjection(Matrix4f projection) {
		setMat4Uniform(PROJECTION, projection);
	}

	// All likely used uniform types are wrapped here

	// Float, float array, float buffer

	public void setFloatUniform(String name, float value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1f(loc, value);
	}

	public void setFloatArrayUniform(String name, float[] value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	public void setFloatBufferUniform(String name, FloatBuffer value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	// Int, int array, int buffer

	public void setIntUniform(String name, int value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1i(loc, value);
	}

	public void setIntArrayUniform(String name, int[] value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	public void setIntBufferUniform(String name, IntBuffer value) {
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	// Vector 2 (all to float)

	public void setVec2Uniform(String name, float x, float y) {
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, x, y);
	}

	public void setVec2Uniform(String name, Vector2f vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, vec.x, vec.y);
	}

	public void setVec2Uniform(String name, double x, double y) {
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, (float) x, (float) y);
	}

	public void setVec2Uniform(String name, Vector2d vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, (float) vec.x, (float) vec.y);
	}

	// Vector 3 (all to float)

	public void setVec3Uniform(String name, float x, float y, float z) {
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, x, y, z);
	}

	public void setVec3Uniform(String name, Vector3f vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, vec.x, vec.y, vec.z);
	}

	public void setVec3Uniform(String name, double x, double y, double z) {
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, (float) x, (float) y, (float) z);
	}

	public void setVec3Uniform(String name, Vector3d vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, (float) vec.x, (float) vec.y, (float) vec.z);
	}

	// Vector 4 (all to float)

	public void setVec4Uniform(String name, float x, float y, float z, float w) {
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, x, y, z, w);
	}

	public void setVec4Uniform(String name, Vector4f vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, vec.x, vec.y, vec.z, vec.w);
	}

	public void setVec4Uniform(String name, double x, double y, double z, float w) {
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, (float) x, (float) y, (float) z, w);
	}

	public void setVec4Uniform(String name, Vector4d vec) {
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, (float) vec.x, (float) vec.y, (float) vec.z, (float) vec.w);
	}

	// Matrix 2, 3, 4

	public void setMat2Uniform(String name, Matrix2f mat) {
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform2fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3f mat) {
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3d mat) {
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat4Uniform(String name, Matrix4f value) {
		int loc = getUniformLocation(name);
		value.get(mat4);
		GL20.glUniformMatrix4fv(loc, false, mat4);
	}

}
