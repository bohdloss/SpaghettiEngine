package com.spaghetti.render;

import com.spaghetti.utils.ExceptionUtil;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

import com.spaghetti.assets.Asset;
import com.spaghetti.core.Game;

public class Model extends Asset {

	public static Model get(String name) {
		return Game.getInstance().getAssetManager().getAndLazyLoadAsset(name);
	}

	public static Model require(String name) {
		return Game.getInstance().getAssetManager().getAndInstantlyLoadAsset(name);
	}

	public static Model getDefault() {
		return Game.getInstance().getAssetManager().getDefaultAsset("Model");
	}

	protected int draw_count;
	protected int v_id, t_id, n_id, i_id;
	protected float[] vertices, normals, tex_coords;
	protected int[] indices;

	public Model() {
	}

	public Model(float[] vertices, float[] tex_coords, float[] normals, int[] indices) {
		setData(vertices, tex_coords, normals, indices);
		load();
	}

	@Override
	public void setData(Object[] objects) {
		if (isValid()) {
			return;
		}

		this.vertices = (float[]) objects[0];
		this.tex_coords = (float[]) objects[1];
		this.normals = (float[]) objects[2];
		this.indices = (int[]) objects[3];

	}

	public void setData(float[] vertices, float[] tex_coords, float[] normals, int[] indices) {
		setData(new Object[] { vertices, tex_coords, normals, indices });
	}

	@Override
	public boolean isFilled() {
		return vertices != null && tex_coords != null && normals != null && indices != null;
	}

	@Override
	protected void load0() {
		if (!isFilled()) {
			throw new IllegalStateException("Invalid data provided for model");
		}

		draw_count = indices.length;
		v_id = GL15.glGenBuffers();
		ExceptionUtil.glError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, v_id);
		ExceptionUtil.glError();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
		ExceptionUtil.glError();
		t_id = GL15.glGenBuffers();
		ExceptionUtil.glError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, t_id);
		ExceptionUtil.glError();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, tex_coords, GL15.GL_STATIC_DRAW);
		ExceptionUtil.glError();
		n_id = GL15.glGenBuffers();
		ExceptionUtil.glError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, n_id);
		ExceptionUtil.glError();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, normals, GL15.GL_STATIC_DRAW);
		ExceptionUtil.glError();
		i_id = GL15.glGenBuffers();
		ExceptionUtil.glError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, i_id);
		ExceptionUtil.glError();
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, indices, GL15.GL_STATIC_DRAW);
		ExceptionUtil.glError();

		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		ExceptionUtil.glError();
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		ExceptionUtil.glError();

	}

	public void render() {
		if (!isValid()) {
			Model base = getDefault();
			if(this != base) {
				base.render();
			}
			return;
		}

		// Enable attributes

		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);

		// Bind buffers

		// Vertices
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, v_id);
		GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0);

		// Texture coordinates
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, t_id);
		GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 0, 0);

		// Normals
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, n_id);
		GL20.glVertexAttribPointer(2, 3, GL11.GL_FLOAT, false, 0, 0);

		// Indices
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, i_id);
		// Actual draw call
		GL11.glDrawElements(GL11.GL_TRIANGLES, draw_count, GL11.GL_UNSIGNED_INT, 0);

		// Unbind buffers

		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		// Disable attributes

		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);

	}

	@Override
	protected void unload0() {
		GL15.glDeleteBuffers(v_id);
		ExceptionUtil.glError();
		GL15.glDeleteBuffers(t_id);
		ExceptionUtil.glError();
		GL15.glDeleteBuffers(n_id);
		ExceptionUtil.glError();
		GL15.glDeleteBuffers(i_id);
		ExceptionUtil.glError();
	}

	public int getDrawCount() {
		return draw_count;
	}

	public int getVertexId() {
		return v_id;
	}

	public int getTextureId() {
		return t_id;
	}

	public int getNormalId() {
		return n_id;
	}

	public int getIndexId() {
		return i_id;
	}

	public float[] getVerticesBuffer() {
		return vertices;
	}

	public float[] getTexcoordsBuffer() {
		return tex_coords;
	}

	public float[] getNormalsBuffer() {
		return normals;
	}

	public int[] getIndicesBuffer() {
		return indices;
	}

}