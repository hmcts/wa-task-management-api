package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
    public static TerminationProcess fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("TerminationProcess value cannot be null");
        }
        for (TerminationProcess terminationProcess : TerminationProcess.values()) {
            if (terminationProcess.getValue().equalsIgnoreCase(value)) {
                return terminationProcess;
            }
        }
        throw new IllegalArgumentException("Unknown TerminationProcess: " + value);
    }
}
