package com.spaghetti.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;
import com.spaghetti.utils.Utils;

public final class ShaderProgram extends Asset {

	public static ShaderProgram get(String name) {
		return Game.getGame().getAssetManager().shaderProgram(name);
	}

	public static ShaderProgram require(String name) {
		return Game.getGame().getAssetManager().requireShaderProgram(name);
	}

	private static final String PROJECTION = "projection";

	protected int id;
	protected Shader[] shaders;

	// cache
	private FloatBuffer mat4 = BufferUtils.createFloatBuffer(16);

	// keep track of valid uniform locations
	private HashMap<String, Integer> locations = new HashMap<>();

	public ShaderProgram() {
	}

	public ShaderProgram(Shader... shaders) {
		Object[] obj = new Object[shaders.length];
		for (int i = 0; i < shaders.length; i++) {
			obj[i] = shaders[i];
		}
		setData(obj);
		load();
	}

	@Override
	public void setData(Object... shaders) {
		if (valid()) {
			return;
		}

		try {

			this.shaders = new Shader[shaders.length];
			for (int i = 0; i < shaders.length; i++) {
				this.shaders[i] = (Shader) shaders[i];
			}

		} catch (Throwable t) {
			this.shaders = null;
			throw t;
		}
	}

	@Override
	public boolean isFilled() {
		return shaders != null;
	}

	@Override
	protected void load0() {
		// Get a usable id for this s-program
		this.id = GL20.glCreateProgram();
		Utils.glError();

		try {

			// Link all shaders
			for (Shader shader : shaders) {
				if (!shader.valid()) {
					throw new IllegalArgumentException("Invalid shader");
				}
				GL20.glAttachShader(id, shader.getId());
				Utils.glError();
			}
			// Bind attributes
			GL20.glBindAttribLocation(id, 0, "vertices");
			Utils.glError();
			GL20.glBindAttribLocation(id, 1, "textures");
			Utils.glError();
			GL20.glBindAttribLocation(id, 2, "normals");
			Utils.glError();

			// Perform linking and check if it worked
			GL20.glLinkProgram(id);
			Utils.glError();

			if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Linker error: " + GL20.glGetProgramInfoLog(id));
			}

			// Perform validation and check if it worked
			GL20.glValidateProgram(id);
			Utils.glError();

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
				Utils.glError();
			}

		}
	}

	public void use() {
		if (!valid()) {
			return;
		}
		GL20.glUseProgram(id);
	}

	@Override
	protected void delete0() {
		GL20.glDeleteProgram(id);
		Utils.glError();
		locations.clear();
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
			Utils.glError();
			if (loc == -1) {
				throw new IllegalArgumentException("Invalid uniform name: " + name);
			}
			locations.put(name, loc);
			return loc;
		}
	}

	public void setMat4Uniform(String name, Matrix4d value) {
		if (!valid()) {
			return;
		}
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

	// Float, float array, float w_buffer

	public void setFloatUniform(String name, float value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1f(loc, value);
	}

	public void setFloatArrayUniform(String name, float[] value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	public void setFloatBufferUniform(String name, FloatBuffer value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	// Int, int array, int w_buffer

	public void setIntUniform(String name, int value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1i(loc, value);
	}

	public void setIntArrayUniform(String name, int[] value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	public void setIntBufferUniform(String name, IntBuffer value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	// Vector 2 (all to float)

	public void setVec2Uniform(String name, float x, float y) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, x, y);
	}

	public void setVec2Uniform(String name, Vector2f vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, vec.x, vec.y);
	}

	public void setVec2Uniform(String name, double x, double y) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, (float) x, (float) y);
	}

	public void setVec2Uniform(String name, Vector2d vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, (float) vec.x, (float) vec.y);
	}

	// Vector 3 (all to float)

	public void setVec3Uniform(String name, float x, float y, float z) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, x, y, z);
	}

	public void setVec3Uniform(String name, Vector3f vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, vec.x, vec.y, vec.z);
	}

	public void setVec3Uniform(String name, double x, double y, double z) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, (float) x, (float) y, (float) z);
	}

	public void setVec3Uniform(String name, Vector3d vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, (float) vec.x, (float) vec.y, (float) vec.z);
	}

	// Vector 4 (all to float)

	public void setVec4Uniform(String name, float x, float y, float z, float w) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, x, y, z, w);
	}

	public void setVec4Uniform(String name, Vector4f vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, vec.x, vec.y, vec.z, vec.w);
	}

	public void setVec4Uniform(String name, double x, double y, double z, double w) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, (float) x, (float) y, (float) z, (float) w);
	}

	public void setVec4Uniform(String name, Vector4d vec) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, (float) vec.x, (float) vec.y, (float) vec.z, (float) vec.w);
	}

	// Matrix 2, 3, 4

	public void setMat2Uniform(String name, Matrix2f mat) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform2fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3f mat) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3d mat) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat4Uniform(String name, Matrix4f value) {
		if (!valid()) {
			return;
		}
		int loc = getUniformLocation(name);
		value.get(mat4);
		GL20.glUniformMatrix4fv(loc, false, mat4);
	}

	@Override
	protected void reset0() {
		shaders = null;
		id = -1;
	}

}
