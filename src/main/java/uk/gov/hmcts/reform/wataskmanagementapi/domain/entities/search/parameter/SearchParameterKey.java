package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SearchParameterKey {

    AVAILABLE_TASKS_ONLY("available_tasks_only", SearchParameterBoolean.class),
    LOCATION("location", SearchParameterList.class),
    USER("user", SearchParameterList.class),
    JURISDICTION("jurisdiction", SearchParameterList.class),
    STATE("state", SearchParameterList.class),
    TASK_ID("taskId", SearchParameterList.class),
    TASK_TYPE("taskType", SearchParameterList.class),
    CASE_ID("caseId", SearchParameterList.class),
    WORK_TYPE("work_type", SearchParameterList.class), //R2 should be snake_case only
    ROLE_CATEGORY("role_category", SearchParameterList.class);

    @JsonValue
    private final String id;

    private final Class<? extends SearchParameter<?>> searchParameterImpl;

    SearchParameterKey(String id, Class<? extends SearchParameter<?>> searchParameterImpl) {
        this.id = id;
        this.searchParameterImpl = searchParameterImpl;
    }

    @Override
    public String toString() {
        return id;
    }

    public String value() {
        return id;
    }

    public Class<? extends SearchParameter<?>> getSearchParameterImpl() {
        return searchParameterImpl;
    }
}
