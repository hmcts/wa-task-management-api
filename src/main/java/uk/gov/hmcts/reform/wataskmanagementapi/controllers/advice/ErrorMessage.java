package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;


import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.http.HttpStatus;

import java.util.Objects;

@ToString
@EqualsAndHashCode
public class ErrorMessage {

    private final String timestamp;
    private final String error;
    private final int status;
    private final String message;

    public ErrorMessage(Exception ex, HttpStatus error, String timestamp) {
        Objects.requireNonNull(ex, "Exception must not be null");
        Objects.requireNonNull(error, "HttpStatus error must not be null");
        Objects.requireNonNull(timestamp, "Timestamp must not be null");
        this.timestamp = timestamp;
        this.error = error.getReasonPhrase();
        this.status = error.value();
        this.message = ex.getMessage();
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getError() {
        return error;
    }

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
