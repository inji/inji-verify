package io.inji.verify.exception;

public class DidGenerationException extends RuntimeException {
    private static final String message = "Error while generating DID document. Please check the certificate and try again.";
    public DidGenerationException() {
        super(message);
    }

    public DidGenerationException(Throwable cause) {
        super(message, cause);
    }
}
