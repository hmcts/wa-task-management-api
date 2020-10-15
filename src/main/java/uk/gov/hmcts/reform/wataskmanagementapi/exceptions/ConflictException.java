package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 7682956120624971223L;

    public ConflictException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
