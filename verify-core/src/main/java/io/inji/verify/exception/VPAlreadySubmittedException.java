package io.inji.verify.exception;

public class VPAlreadySubmittedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

	public VPAlreadySubmittedException() {
        super("VP request has already been submitted");
    }

    public VPAlreadySubmittedException(String message) {
        super(message);
    }

    public VPAlreadySubmittedException(String message, Throwable cause) {
        super(message, cause);
    }

    public VPAlreadySubmittedException(Throwable cause) {
        super(cause);
    }
}