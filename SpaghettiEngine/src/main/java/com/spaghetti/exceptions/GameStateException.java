package com.spaghetti.exceptions;

public class GameStateException extends RuntimeException {

	private static final long serialVersionUID = -6286875609043987901L;

	public GameStateException(String message) {
		super(message);
	}

	public GameStateException(String message, Throwable cause) {
		super(message, cause);
	}

	public GameStateException(Throwable cause) {
		super(cause);
	}

}
