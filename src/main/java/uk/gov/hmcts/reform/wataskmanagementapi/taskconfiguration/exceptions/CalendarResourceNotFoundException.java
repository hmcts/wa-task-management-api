package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions;

public class CalendarResourceNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 6753971692367781003L;

    public CalendarResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
