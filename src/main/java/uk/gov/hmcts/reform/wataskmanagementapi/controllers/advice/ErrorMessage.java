package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;


import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class ErrorMessage {

    private String timestamp;
    private String error;
    private int status;
    private String message;

    public ErrorMessage(Exception ex, HttpStatus error) {
        this.timestamp = ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
        this.error = error.getReasonPhrase();
        this.status = error.value();
        this.message = ex.getMessage();
    }
}
