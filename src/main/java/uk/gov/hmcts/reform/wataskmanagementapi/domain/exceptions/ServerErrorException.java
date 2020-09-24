package uk.gov.hmcts.reform.wataskmanagementapi.domain.exceptions;

public class ServerErrorException extends RuntimeException {

    private static final long serialVersionUID = -6933665568015006662L;

    public ServerErrorException(
        String message,
        Throwable cause
    ) {
        super(message, cause);
    }
}
