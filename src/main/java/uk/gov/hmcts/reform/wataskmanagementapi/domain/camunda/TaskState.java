package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonValue;

public enum TaskState {
    UNCONFIGURED("unconfigured"),
    UNASSIGNED("unassigned"),
    CONFIGURED("configured"),
    ASSIGNED("assigned"),
    REFERRED("referred"),
    COMPLETED("completed"),
    CANCELLED("cancelled");

    @JsonValue
    private final String value;

    TaskState(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
