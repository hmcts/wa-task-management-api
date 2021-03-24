package uk.gov.hmcts.reform.wataskmanagementapi.controllers.advice;

import java.util.Objects;

public class ErrorMessageSummary {

    private final String message;

    public ErrorMessageSummary(Exception ex) {
        Objects.requireNonNull(ex, "Exception must not be null");
        this.message = ex.getMessage();

    }

    public String getMessage() {
        return message;
    }
}
