package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.annotation.JsonValue;

public enum CamundaMessage {

    CREATE_TASK_MESSAGE("createTaskMessage");

    @JsonValue
    private final String id;

    CamundaMessage(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
