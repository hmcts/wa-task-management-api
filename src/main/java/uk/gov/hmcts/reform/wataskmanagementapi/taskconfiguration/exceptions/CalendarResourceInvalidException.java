package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions;

public class CalendarResourceInvalidException extends RuntimeException{
    public CalendarResourceInvalidException(String message, Throwable cause) {
        super(message, cause);
    }
}
