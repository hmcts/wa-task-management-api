package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchParameterKey {

    AVAILABLE_TASKS_ONLY("available_tasks_only"),
    LOCATION("location"),
    USER("user"),
    JURISDICTION("jurisdiction"),
    STATE("state"),
    TASK_ID("taskId"),
    TASK_TYPE("taskType"),
    CASE_ID("caseId"),
    WORK_TYPE("work_type"), //R2 should be snake_case only
    ROLE_CATEGORY("role_category");

    @JsonValue
    private final String id;

    SearchParameterKey(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

    public String value() {
        return id;
    }

}
