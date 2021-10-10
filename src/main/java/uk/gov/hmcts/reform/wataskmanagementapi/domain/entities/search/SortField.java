package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum SortField {

    DUE_DATE_CAMEL_CASE("dueDate", "dueDate"),
    DUE_DATE_SNAKE_CASE("due_date", "dueDate"),

    TASK_TITLE_CAMEL_CASE("taskTitle", "title"),
    TASK_TITLE_SNAKE_CASE("task_title", "title"),

    LOCATION_NAME_CAMEL_CASE("locationName", "locationName"),
    LOCATION_NAME_SNAKE_CASE("location_name", "locationName"),

    CASE_CATEGORY_CAMEL_CASE("caseCategory", "appealType"),
    CASE_CATEGORY_SNAKE_CASE("case_category", "appealType"),

    CASE_ID_CAMEL_CASE("caseId", "caseId"),
    CASE_ID_SNAKE_CASE("case_id", "caseId"),

    CASE_NAME_CAMEL_CASE("caseName", "caseName"),
    CASE_NAME_SNAKE_CASE("case_name", "caseName"),

    WORK_TYPE_CAMEL_CASE("workType", "workType"),
    WORK_TYPE_SNAKE_CASE("work_type", "workType");

    @JsonValue
    private final String id;
    private final String camundaVariableName;

    SortField(String id, String camundaVariableName) {
        this.id = id;
        this.camundaVariableName = camundaVariableName;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
