package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class InsufficientPermissionsException extends RuntimeException {

    private static final long serialVersionUID = -285196365014769835L;

    public InsufficientPermissionsException(
        String message
    ) {
        this(message, null);
    }

    public InsufficientPermissionsException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
