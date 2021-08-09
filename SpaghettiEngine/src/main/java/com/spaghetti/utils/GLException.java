package com.spaghetti.utils;

public class GLException extends RuntimeException {

	private static final long serialVersionUID = -878596842738489954L;
	protected int source, type, severity, id;
	protected String message;

	public GLException(int source, int type, int severity, int id, String message) {
		super(message);
		this.source = source;
		this.type = type;
		this.severity = severity;
		this.id = id;
	}

	public int getSource() {
		return source;
	}

	public int getType() {
		return type;
	}

	public int getSeverity() {
		return severity;
	}

	public int getId() {
		return id;
	}

	public String getGlMessage() {
		return message;
	}

}