package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;


import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ErrorMessage {

    private LocalDateTime timestamp;
    private String error;
    private int status;
    private String message;

    public ErrorMessage(Exception ex, HttpStatus error, LocalDateTime timestamp) {
        this.timestamp = timestamp;
        this.error = error.getReasonPhrase();
        this.status = error.value();
        this.message = ex.getMessage();
    }
}
