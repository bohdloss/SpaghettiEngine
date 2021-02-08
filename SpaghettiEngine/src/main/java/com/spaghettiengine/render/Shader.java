package com.spaghettiengine.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL40;

import com.spaghettiengine.assets.Asset;
import com.spaghettiengine.core.Game;

public final class Shader extends Asset {

	public static Shader get(String name) {
		return Game.getGame().getAssetManager().shader(name);
	}

	public static Shader require(String name) {
		return Game.getGame().getAssetManager().requireShader(name);
	}

	public static final int VERTEX_SHADER = GL20.GL_VERTEX_SHADER;
	public static final int FRAGMENT_SHADER = GL20.GL_FRAGMENT_SHADER;
	public static final int GEOMETRY_SHADER = GL32.GL_GEOMETRY_SHADER;
	public static final int TESS_CONTROL_SHADER = GL40.GL_TESS_CONTROL_SHADER;
	public static final int TESS_EVALUATION_SHADER = GL40.GL_TESS_EVALUATION_SHADER;

	protected int id;
	protected String source;
	protected int type;

	private static final boolean validateType(int type) {
		return type == VERTEX_SHADER || type == FRAGMENT_SHADER || type == GEOMETRY_SHADER
				|| type == TESS_CONTROL_SHADER || type == TESS_EVALUATION_SHADER;
	}

	public Shader() {
	}

	public Shader(String source, int type) {
		setData(source, type);
		load();
	}

	public void setData(Object...objects) {
		if (valid()) {
			return;
		}

		this.source = (String) objects[0];
		this.type = (int) objects[1];
	}

	@Override
	public boolean isFilled() {
		return (type == VERTEX_SHADER ||
				type == FRAGMENT_SHADER ||
				type == GEOMETRY_SHADER ||
				type == TESS_CONTROL_SHADER ||
				type == TESS_EVALUATION_SHADER) &&
				source != null;
	}
	
	@Override
	protected void load0() {
		if (!validateType(type) || source == null) {
			throw new IllegalArgumentException("Invalid type or null source");
		}

		// Create usable shader id
		id = GL20.glCreateShader(type);

		try {

			GL20.glShaderSource(id, source);
			GL20.glCompileShader(id);

			if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
				throw new IllegalArgumentException("Compiler error: " + GL20.glGetShaderInfoLog(id));
			}

		} catch (Throwable t) {

			// Clean up first
			delete();

			// Then throw
			throw t;

		}
	}

	@Override
	protected void delete0() {
		GL20.glDeleteShader(id);
	}

	public int getId() {
		return isDeleted() ? 0 : id;
	}

	public String getSource() {
		return source;
	}

	public int getType() {
		return type;
	}

	@Override
	protected void reset0() {
		id = -1;
		source = null;
		type = -1;
	}

}
