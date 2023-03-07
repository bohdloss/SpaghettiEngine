package com.spaghetti.exceptions;

public class EndpointException extends RuntimeException {

	private static final long serialVersionUID = -4847107295887349655L;

	public EndpointException(String message) {
		super(message);
	}

	public EndpointException(String message, Throwable cause) {
		super(message, cause);
	}

	public EndpointException(Throwable cause) {
		super(cause);
	}

}
