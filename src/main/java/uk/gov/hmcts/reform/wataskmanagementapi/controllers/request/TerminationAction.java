package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum TerminationAction {
    CANCEL("cancel"),
    COMPLETE("complete");

    private final String value;

    TerminationAction(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static TerminationAction fromValue(String value) {
        return Arrays.stream(values())
            .filter(action -> action.value.equalsIgnoreCase(value) || action.name().equalsIgnoreCase(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown TerminationAction: " + value));
    }
}
