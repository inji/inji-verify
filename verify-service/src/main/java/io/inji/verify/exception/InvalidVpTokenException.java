package io.inji.verify.exception;

public class InvalidVpTokenException extends RuntimeException {
    private static final String message = "Invalid VP token";

    public InvalidVpTokenException() {
        super(message);
    }
}