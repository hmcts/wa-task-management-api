package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class NoRoleAssignmentsFoundException extends RuntimeException {

    private static final long serialVersionUID = -1452010004310037746L;

    public NoRoleAssignmentsFoundException(
        String message
    ) {
        super(message);
    }
    public NoRoleAssignmentsFoundException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
