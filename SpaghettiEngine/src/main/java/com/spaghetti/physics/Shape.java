package com.spaghetti.physics;

import java.util.ArrayList;

public class Shape<VecType> {

	protected ArrayList<VecType> vertices = new ArrayList<>();
	protected float radius;

	public Shape() {
	}
	
	public Shape(VecType[] vertices) {
		addVertices(vertices);
	}

	// Getters

	public void clear() {
		vertices.clear();
		radius = 0;
	}
	
	public int getVertexCount() {
		return vertices.size();
	}

	public VecType getVertex(int index) {
		return vertices.get(index);
	}

	public void setVertex(VecType vertex, int index) {
		vertices.set(index, vertex);
	}
	
	public void addVertex(VecType vertex, int index) {
		vertices.add(index, vertex);
	}
	
	public void addVertex(VecType vertex) {
		vertices.add(vertex);
	}
	
	public void removeVertex(int index) {
		vertices.remove(index);
	}
	
	public void addVertices(VecType[] buffer, int index) {
		int i = index;
		for(VecType vec : buffer) {
			vertices.add(i++, vec);
		}
	}
	
	public void addVertices(VecType[] buffer) {
		for(VecType vec : buffer) {
			vertices.add(vec);
		}
	}
	
	public float getRadius() {
		return radius;
	}
	
	public void setRadius(float radius) {
		this.radius = radius;
	}
	
	public boolean isCircle() {
		return radius > 0;
	}
	
	public boolean isValid() {
		return isCircle() || vertices.size() > 2;
	}
	
}