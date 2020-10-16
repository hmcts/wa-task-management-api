package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -1899364441389962749L;

    public ResourceNotFoundException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
