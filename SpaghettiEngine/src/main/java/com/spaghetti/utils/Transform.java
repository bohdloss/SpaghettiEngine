package com.spaghetti.utils;

import org.joml.Vector3f;

public class Transform {

	public Vector3f position = new Vector3f();
	public Vector3f rotation = new Vector3f();
	public Vector3f scale = new Vector3f(1);

	public void set(Transform other) {
		position.set(other.position);
		rotation.set(other.rotation);
		scale.set(other.scale);
	}

	public void add(Transform other) {
		position.add(other.position);
		rotation.add(other.rotation);
		scale.add(other.scale);
	}

	public void sub(Transform other) {
		position.sub(other.position);
		rotation.sub(other.rotation);
		scale.sub(other.scale);
	}

	public void mul(Transform other) {
		position.mul(other.position);
		rotation.mul(other.rotation);
		scale.mul(other.scale);
	}

	public void div(Transform other) {
		position.div(other.position);
		rotation.div(other.rotation);
		scale.div(other.scale);
	}

	public void add(float value, Transform buffer) {
		position.add(value, value, value, buffer.position);
		rotation.add(value, value, value, buffer.rotation);
		scale.add(value, value, value, buffer.scale);
	}

	public void sub(float value, Transform buffer) {
		position.sub(value, value, value, buffer.position);
		rotation.sub(value, value, value, buffer.rotation);
		scale.sub(value, value, value, buffer.scale);
	}

	public void mul(float value, Transform buffer) {
		position.mul(value, buffer.position);
		rotation.mul(value, buffer.rotation);
		scale.mul(value, buffer.scale);
	}

	public void div(float value, Transform buffer) {
		position.div(value, buffer.position);
		rotation.div(value, buffer.rotation);
		scale.div(value, buffer.scale);
	}

	public void add(float value) {
		position.add(value, value, value);
		rotation.add(value, value, value);
		scale.add(value, value, value);
	}

	public void sub(float value) {
		position.sub(value, value, value);
		rotation.sub(value, value, value);
		scale.sub(value, value, value);
	}

	public void mul(float value) {
		position.mul(value);
		rotation.mul(value);
		scale.mul(value);
	}

	public void div(float value) {
		position.div(value);
		rotation.div(value);
		scale.div(value);
	}

}
