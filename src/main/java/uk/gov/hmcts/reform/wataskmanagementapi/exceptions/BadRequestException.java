package uk.gov.hmcts.reform.wataskmanagementapi.exceptions;

public class BadRequestException extends RuntimeException {

    private static final long serialVersionUID = -6933665568015406662L;

    public BadRequestException(
        String message
    ) {
        super(message);
    }
}
