package io.inji.verify.exception;

public class JWTCreationException extends RuntimeException {
    private static final String message = "Error while creating JWT. Please check the input and try again.";

    public JWTCreationException() {
        super(message);
    }
}
