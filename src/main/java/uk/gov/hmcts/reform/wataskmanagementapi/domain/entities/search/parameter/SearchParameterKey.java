package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * If adding a new field please ensure they are added as snake_case only and not camelCase.
 * The camelCase format is deprecated and will be removed.
 */
public enum SearchParameterKey {

    LOCATION("location"),
    USER("user"),
    JURISDICTION("jurisdiction"),
    STATE("state"),
    TASK_TYPE("task_type"),
    CASE_ID_CAMEL_CASE("caseId"),
    CASE_ID("case_id"),
    //R2 should be snake_case only,
    WORK_TYPE("work_type"),
    AVAILABLE_TASKS_ONLY("available_tasks_only"),
    ROLE_CATEGORY("role_category"),
    REQUEST_CONTEXT("request_context");

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
