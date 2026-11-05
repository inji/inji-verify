package exceptions;

/** Indicates that no configured credential key or display name matches the input. */
public class UnknownCredentialException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public UnknownCredentialException(String message) {
        super(message);
    }
}
