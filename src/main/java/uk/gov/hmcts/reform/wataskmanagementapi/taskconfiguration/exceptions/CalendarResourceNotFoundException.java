package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.exceptions;

import feign.FeignException;

public class CalendarResourceNotFoundException extends RuntimeException{
    public CalendarResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
