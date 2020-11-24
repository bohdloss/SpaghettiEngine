package com.spaghettiengine.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

public final class Shader {

	public static final int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
	public static final int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
	public static final int GEOMETRY_SHADER = GL32.GL_GEOMETRY_SHADER;
	public static final int TESS_CONTROL_SHADER = GL40.GL_TESS_CONTROL_SHADER;
	public static final int TESS_EVALUATION_SHADER = GL40.GL_TESS_EVALUATION_SHADER;

	protected final int id;
	private boolean deleted;

	private static final boolean validateType(int type) {
		return type == VERTEX_SHADER || type == FRAGMENT_SHADER || type == GEOMETRY_SHADER
				|| type == TESS_CONTROL_SHADER || type == TESS_EVALUATION_SHADER;
	}

	public Shader(String source, int type) {
		if (!validateType(type) || source == null) {
			throw new IllegalArgumentException("Invalid type or null source");
		}

		id = GL20.glCreateShader(type);
		GL20.glShaderSource(id, source);
		GL20.glCompileShader(id);

		if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			delete();
			throw new IllegalArgumentException("Compiler error: " + GL20.glGetShaderInfoLog(id));
		}
	}

	public void delete() {
		if (deleted) {
			return;
		}

		GL20.glDeleteShader(id);

		deleted = true;
	}

	public int getId() {
		return deleted ? 0 : id;
	}

	public boolean isDeleted() {
		return deleted;
	}

}
