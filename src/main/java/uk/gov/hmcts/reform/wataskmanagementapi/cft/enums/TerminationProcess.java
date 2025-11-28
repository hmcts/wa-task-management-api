package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum TerminationProcess {

    EXUI_USER_COMPLETION("EXUI_USER_COMPLETION"),
    EXUI_CASE_EVENT_COMPLETION("EXUI_CASE-EVENT_COMPLETION"),
    EXUI_USER_CANCELLATION("EXUI_USER_CANCELLATION"),
    EXUI_CASE_EVENT_CANCELLATION("CASE_EVENT_CANCELLATION");

    private final String value;


    TerminationProcess(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Optional<TerminationProcess> fromValue(String value) {
        for (TerminationProcess terminationProcess : TerminationProcess.values()) {
            if (terminationProcess.getValue().equalsIgnoreCase(value)) {
                return Optional.of(terminationProcess);
            }
        }
        return Optional.empty();
    }
}
