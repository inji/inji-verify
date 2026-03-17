package io.inji.verify.exception;

public class MalformedCookieException extends RuntimeException {
    public MalformedCookieException(IllegalArgumentException e) {
        super(e.getMessage());
    }
}
