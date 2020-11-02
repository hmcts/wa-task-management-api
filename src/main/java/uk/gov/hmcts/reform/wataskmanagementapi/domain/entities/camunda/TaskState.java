package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

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
    private final String taskState;

    TaskState(String taskState) {
        this.taskState = taskState;
    }

    public String getTaskState() {
        return this.taskState;
    }

    @Override
    public String toString() {
        return this.taskState;
    }
}
