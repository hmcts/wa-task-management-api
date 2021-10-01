package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class RequireDbLockException extends RuntimeException {

    private static final long serialVersionUID = 2970088370231300192L;

    public RequireDbLockException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
