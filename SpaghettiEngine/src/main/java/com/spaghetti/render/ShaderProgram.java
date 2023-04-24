package com.spaghetti.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

import com.spaghetti.utils.ExceptionUtil;
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
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;

public final class ShaderProgram extends Asset {

	public static ShaderProgram get(String name) {
		return Game.getInstance().getAssetManager().getAndLazyLoadAsset(name);
	}

	public static ShaderProgram require(String name) {
		return Game.getInstance().getAssetManager().getAndInstantlyLoadAsset(name);
	}

	public static ShaderProgram getDefault() {
		return Game.getInstance().getAssetManager().getDefaultAsset("ShaderProgram");
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
	public void setData(Object[] shaders) {
		if (isLoaded()) {
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
		ExceptionUtil.glConsumeError();
		// Get a usable id for this s-program
		this.id = GL20.glCreateProgram();
		ExceptionUtil.glError();

		try {

			// Link all shaders
			for (Shader shader : shaders) {
				if (!shader.isLoaded()) {
					throw new IllegalArgumentException("Invalid shader");
				}
				GL20.glAttachShader(id, shader.getId());
				ExceptionUtil.glError();
			}
			// Bind attributes
			GL20.glBindAttribLocation(id, 0, "vertices");
			ExceptionUtil.glError();
			GL20.glBindAttribLocation(id, 1, "textures");
			ExceptionUtil.glError();
			GL20.glBindAttribLocation(id, 2, "normals");
			ExceptionUtil.glError();

			// Perform linking and check if it worked
			GL20.glLinkProgram(id);
			ExceptionUtil.glError();

			if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Linker error: " + GL20.glGetProgramInfoLog(id));
			}

			// Perform validation and check if it worked
			GL20.glValidateProgram(id);
			ExceptionUtil.glError();

			if (GL20.glGetProgrami(id, GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Validator error: " + GL20.glGetProgramInfoLog(id));
			}

		} catch (Throwable t) {

			// In case an error occurs, avoid leaking memory
			unload();

			// Pass the throwable back up the stack
			throw t;

		} finally {

			// Whatever happens, shaders should be detached
			for (Shader shader : shaders) {
				GL20.glDetachShader(id, shader.getId());
				ExceptionUtil.glError();
			}

		}
	}

	public void use() {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if (this != base) {
				base.use();
			}
			return;
		}
		GL20.glUseProgram(id);
	}

	@Override
	protected void unload0() {
		ExceptionUtil.glConsumeError();
		GL20.glDeleteProgram(id);
		ExceptionUtil.glError();
		locations.clear();
	}

	public int getId() {
		return isLoaded() ? id : getDefault().id;
	}

	private int getUniformLocation(String name) {
		Integer location = locations.get(name);
		if (location != null) {
			// The location is cached
			return location;
		} else {
			// Find the location and cache it
			GL11.glGetError();
			int loc = GL20.glGetUniformLocation(id, name);
			ExceptionUtil.glError();
			if (loc == -1) {
				throw new IllegalArgumentException("Invalid uniform name: " + name);
			}
			locations.put(name, loc);
			return loc;
		}
	}

	// These are the most commonly used uniform methods and so are on top

	public void setMat4Uniform(String name, Matrix4d value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setMat4Uniform(name, value);
			}
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

	public void setCullFace(boolean cullFace) {
		GL20.glCullFace(cullFace ? GL20.GL_BACK : GL20.GL_FRONT);
	}

	// All likely used uniform types are wrapped here

	// Float, float array, float w_buffer

	public void setFloatUniform(String name, float value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setFloatUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1f(loc, value);
	}

	public void setFloatArrayUniform(String name, float[] value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setFloatArrayUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	public void setFloatBufferUniform(String name, FloatBuffer value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setFloatBufferUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1fv(loc, value);
	}

	// Int, int array, int w_buffer

	public void setIntUniform(String name, int value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setIntUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1i(loc, value);
	}

	public void setIntArrayUniform(String name, int[] value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setIntArrayUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	public void setIntBufferUniform(String name, IntBuffer value) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setIntBufferUniform(name, value);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform1iv(loc, value);
	}

	// Vector 2 (all to float)

	public void setVec2Uniform(String name, float x, float y) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec2Uniform(name, x, y);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, x, y);
	}

	public void setVec2Uniform(String name, Vector2f vec) {
		setVec2Uniform(name, vec.x, vec.y);
	}

	public void setVec2Uniform(String name, double x, double y) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec2Uniform(name, x, y);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform2f(loc, (float) x, (float) y);
	}

	public void setVec2Uniform(String name, Vector2d vec) {
		setVec2Uniform(name, vec.x, vec.y);
	}

	// Vector 3 (all to float)

	public void setVec3Uniform(String name, float x, float y, float z) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec3Uniform(name, x, y, z);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, x, y, z);
	}

	public void setVec3Uniform(String name, Vector3f vec) {
		setVec3Uniform(name, vec.x, vec.y, vec.z);
	}

	public void setVec3Uniform(String name, double x, double y, double z) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec3Uniform(name, x, y, z);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform3f(loc, (float) x, (float) y, (float) z);
	}

	public void setVec3Uniform(String name, Vector3d vec) {
		setVec3Uniform(name, vec.x, vec.y, vec.z);
	}

	// Vector 4 (all to float)

	public void setVec4Uniform(String name, float x, float y, float z, float w) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec4Uniform(name, x, y, z, w);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, x, y, z, w);
	}

	public void setVec4Uniform(String name, Vector4f vec) {
		setVec4Uniform(name, vec.x, vec.y, vec.z, vec.w);
	}

	public void setVec4Uniform(String name, double x, double y, double z, double w) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setVec4Uniform(name, x, y, z, w);
			}
			return;
		}
		int loc = getUniformLocation(name);
		GL20.glUniform4f(loc, (float) x, (float) y, (float) z, (float) w);
	}

	public void setVec4Uniform(String name, Vector4d vec) {
		setVec4Uniform(name, vec.x, vec.y, vec.z, vec.w);
	}

	// Matrix 2, 3, 4

	public void setMat2Uniform(String name, Matrix2f mat) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setMat2Uniform(name, mat);
			}
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform2fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3f mat) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setMat3Uniform(name, mat);
			}
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat3Uniform(String name, Matrix3d mat) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setMat3Uniform(name, mat);
			}
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniform3fv(loc, mat4);
	}

	public void setMat4Uniform(String name, Matrix4f mat) {
		if (!isLoaded()) {
			ShaderProgram base = getDefault();
			if(this != base) {
				base.setMat4Uniform(name, mat);
			}
			return;
		}
		int loc = getUniformLocation(name);
		mat.get(mat4);
		GL20.glUniformMatrix4fv(loc, false, mat4);
	}

}
