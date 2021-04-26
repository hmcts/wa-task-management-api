package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import com.fasterxml.jackson.annotation.JsonValue;

public enum SortField {

    DUE_DATE("dueDate"),
    TASK_TITLE("taskTitle"),
    LOCATION_NAME("locationName"),
    CASE_CATEGORY("caseCategory"),
    CASE_ID("caseId"),
    CASE_NAME("caseName");

    @JsonValue
    private final String id;

    SortField(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
