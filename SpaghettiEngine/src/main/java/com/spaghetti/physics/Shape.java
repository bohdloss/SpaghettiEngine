package com.spaghetti.physics;

import java.util.ArrayList;

/**
 * Shape defines how a body interacts with other bodies in the physics world
 * with either a list of vertices or a radius for circles and spheres
 *
 * @author bohdloss
 *
 * @param <VecType> Children classes are required to specify their own
 *                  positional vector class
 */
public class Shape<VecType> implements Cloneable {

	protected ArrayList<VecType> vertices = new ArrayList<>();
	protected float radius;

	/**
	 * No action is performed
	 */
	public Shape() {
	}

	/**
	 * Functionally equal to creating an instance with the empty constructor and
	 * then calling {@link Shape#addVertices(Object[])} with {@code vertices} as the
	 * parameter
	 *
	 * @param vertices The vertices to initialize the shape with
	 */
	public Shape(VecType[] vertices) {
		addVertices(vertices);
	}

	/**
	 * Functionally equal to creating an instance with the empty constructor and
	 * then calling {@link Shape#setRadius(float)} with {@code radius} as the
	 * parameter
	 *
	 * @param radius The radius to initialize the shape with
	 */
	public Shape(float radius) {
		setRadius(radius);
	}

	// Getters

	/**
	 * Resets this Shape to its initial state as if it was created with the empty
	 * constructor
	 */
	public void clear() {
		vertices.clear();
		radius = 0;
	}

	/**
	 * Retrieves the amount of vertices in this shape
	 *
	 * @return The vertex count
	 */
	public int getVertexCount() {
		return vertices.size();
	}

	/**
	 * Retrieves the vertex of the shape at the given index as a vector
	 * <p>
	 * Modifying this vector is allowed and will reflect back to the shape
	 *
	 * @param index The index to retrieve the point from
	 * @return A vector representing the vertex
	 */
	public VecType getVertex(int index) {
		return vertices.get(index);
	}

	/**
	 * Changes a point of the shape at the given index to the vertex parameter
	 * <p>
	 * Later changes made to the vector will reflect back to the shape
	 *
	 * @param vertex The new vertex
	 * @param index  The index to replace
	 */
	public void setVertex(VecType vertex, int index) {
		vertices.set(index, vertex);
	}

	/**
	 * Inserts a single vertex in the shape at the given index
	 * <p>
	 * Later changes made to the vector will reflect back to the shape
	 *
	 * @param vertex The vertex to insert
	 * @param index  The index to insert the vertex at
	 */
	public void addVertex(VecType vertex, int index) {
		vertices.add(index, vertex);
	}

	/**
	 * Adds a single vertex to the shape
	 * <p>
	 * Later changes made to the vector will reflect back to the shape
	 *
	 * @param vertex The vertex to add
	 */
	public void addVertex(VecType vertex) {
		vertices.add(vertex);
	}

	/**
	 * Removes a single vertex from the shape, at the given index
	 *
	 * @param index The index of the vertex to remove
	 */
	public void removeVertex(int index) {
		vertices.remove(index);
	}

	/**
	 * Inserts multiple vertices at the given index in the shape
	 *
	 * @param buffer An array of vertices to be inserted
	 * @param index  The index at which to insert them
	 */
	public void addVertices(VecType[] buffer, int index) {
		int i = index;
		for (VecType vec : buffer) {
			vertices.add(i++, vec);
		}
	}

	/**
	 * Adds multiple vertices to the shape
	 *
	 * @param buffer An array of vertices to be added
	 */
	public void addVertices(VecType[] buffer) {
		for (VecType vec : buffer) {
			vertices.add(vec);
		}
	}

	/**
	 * Retrieves the radius of the shape
	 *
	 * @return The radius
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * Changes the radius of the shape
	 *
	 * @param radius The new radius
	 */
	public void setRadius(float radius) {
		this.radius = radius;
	}

	/**
	 * Returns true if there is a stored radius that is greater than zero
	 *
	 * @return {@link Shape#getRadius()} {@code > 0}
	 */
	public boolean isCircle() {
		return radius > 0;
	}

	/**
	 * Returns true if this is a circle with a radius greater than zero or if it's a
	 * polygon shape with at least 3 vertices
	 *
	 * @return {@link Shape#isCircle()} {@code || } {@link Shape#getVertexCount()}
	 *         {@code > 2}
	 */
	public boolean isValid() {
		return isCircle() || vertices.size() > 2;
	}

	@Override
	public Shape<VecType> clone() {
		Shape<VecType> clone = new Shape<>();
		clone.radius = radius;
		for (VecType vec : vertices) {
			clone.vertices.add(vec);
		}
		return clone;
	}

	public void set(Shape<VecType> other) {
		clear();
		radius = other.radius;
		for (VecType vec : other.vertices) {
			vertices.add(vec);
		}
	}

}