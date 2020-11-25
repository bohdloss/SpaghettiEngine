package com.spaghettiengine.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

public class Model {

	protected final int draw_count;
	protected final int v_id;
	private final int t_id;
	private final int i_id;
		
	public Model(float[] vertices, float[] tex_coords, int[] indices) {
		draw_count = indices.length;
		v_id = GL20.glGenBuffers();
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, v_id);
		GL20.glBufferData(GL20.GL_ARRAY_BUFFER, createBuffer(vertices), GL20.GL_STATIC_DRAW);
		t_id = GL20.glGenBuffers();
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, t_id);
		GL20.glBufferData(GL20.GL_ARRAY_BUFFER, createBuffer(tex_coords), GL20.GL_STATIC_DRAW);
		i_id = GL20.glGenBuffers();
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, i_id);
		
		IntBuffer buffer = BufferUtils.createIntBuffer(indices.length);
		buffer.put(indices);
		buffer.flip();
		
		GL20.glBufferData(GL20.GL_ARRAY_BUFFER, buffer, GL20.GL_STATIC_DRAW);

		GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
	}

	public void render() {
		
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, v_id);
		GL20.glVertexAttribPointer(0, 3, GL20.GL_FLOAT, false, 0, 0);
		
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, t_id);
		GL20.glVertexAttribPointer(1, 2, GL20.GL_FLOAT, false, 0, 0);
		
		GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, i_id);
		GL20.glDrawElements(GL20.GL_TRIANGLES, draw_count, GL20.GL_UNSIGNED_INT, 0);
		
		GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		
	}

	private FloatBuffer createBuffer(float[] data) {
		FloatBuffer buffer = BufferUtils.createFloatBuffer(data.length);
		buffer.put(data);
		buffer.flip();
		return buffer;
	}
	
}
