package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class UnAuthorizedException extends RuntimeException {

    private static final long serialVersionUID = 3295134713960168276L;

    public UnAuthorizedException(
        String message
    ) {
        super(message);
    }

    public UnAuthorizedException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
