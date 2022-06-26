package com.spaghetti.utils;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

public class GLFWException extends RuntimeException {

	private static final long serialVersionUID = -878596842738489954L;
	protected int error;
	protected String description;

	protected static String errorString(int error, long description) {
		String string;
		switch (error) {
		default:
			string = "Unknown error";
			break;
		case GLFW.GLFW_NOT_INITIALIZED:
			string = "GLFW_NOT_INITIALIZED";
			break;
		case GLFW.GLFW_NO_CURRENT_CONTEXT:
			string = "GLFW_NO_CURRENT_CONTEXT";
			break;
		case GLFW.GLFW_INVALID_ENUM:
			string = "GLFW_INVALID_ENUM";
			break;
		case GLFW.GLFW_INVALID_VALUE:
			string = "GLFW_INVALID_VALUE";
			break;
		case GLFW.GLFW_OUT_OF_MEMORY:
			string = "GLFW_OUT_OF_MEMORY";
			break;
		case GLFW.GLFW_API_UNAVAILABLE:
			string = "GLFW_API_UNAVAILABLE";
			break;
		case GLFW.GLFW_VERSION_UNAVAILABLE:
			string = "GLFW_VERSION_UNAVAILABLE";
			break;
		case GLFW.GLFW_PLATFORM_ERROR:
			string = "GLFW_PLATFORM_ERROR";
			break;
		case GLFW.GLFW_FORMAT_UNAVAILABLE:
			string = "GLFW_FORMAT_UNAVAILABLE";
			break;
		case GLFW.GLFW_NO_WINDOW_CONTEXT:
			string = "GLFW_NO_WINDOW_CONTEXT";
			break;
		}
		string += ": ";
		string += MemoryUtil.memUTF8(description);

		return string;
	}

	public GLFWException(int error, long description) {
		super(errorString(error, description));
		this.error = error;
		this.description = MemoryUtil.memUTF8(description);
	}

	public int getError() {
		return error;
	}

	public String getDescription() {
		return description;
	}

}