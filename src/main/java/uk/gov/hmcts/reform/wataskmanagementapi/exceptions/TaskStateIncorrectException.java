package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class TaskStateIncorrectException extends RuntimeException {

    private static final long serialVersionUID = -7841443483082913542L;

    public TaskStateIncorrectException(
        String message
    ) {
        super(message);
    }

    public TaskStateIncorrectException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
