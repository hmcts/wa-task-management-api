package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

public enum SortField {

    DUE_DATE("dueDate", "dueDate"),
    TASK_TITLE("taskTitle", "title"),
    LOCATION_NAME("locationName", "locationName"),
    CASE_CATEGORY("caseCategory", "appealType"),
    CASE_ID("caseId", "caseId"),
    CASE_NAME("caseName", "caseName");

    @JsonValue
    private final String id;
    @Getter
    private final String varName;

    SortField(String id, String varName) {
        this.id = id;
        this.varName = varName;
    }

}
