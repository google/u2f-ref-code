package com.google.u2f;

@SuppressWarnings("serial")
public class U2FException extends Exception {

	public U2FException(String message) {
		super(message);
	}

	public U2FException(String message, Throwable cause) {
		super(message, cause);
	}
}
