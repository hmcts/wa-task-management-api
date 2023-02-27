package uk.gov.hmcts.reform.wataskmanagementapi.domain.search;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@SuppressWarnings({"squid:S1192", "PMD.AvoidDuplicateLiterals"})
@Getter
public enum SortField {

    DUE_DATE_CAMEL_CASE("dueDate", "dueDate", "dueDateTime"),
    DUE_DATE_SNAKE_CASE("due_date", "dueDate", "dueDateTime"),

    TASK_TITLE_CAMEL_CASE("taskTitle", "title", "title"),
    TASK_TITLE_SNAKE_CASE("task_title", "title", "title"),

    LOCATION_NAME_CAMEL_CASE("locationName", "locationName", "locationName"),
    LOCATION_NAME_SNAKE_CASE("location_name", "locationName", "locationName"),

    CASE_CATEGORY_CAMEL_CASE("caseCategory", "appealType", "caseCategory"),
    CASE_CATEGORY_SNAKE_CASE("case_category", "appealType", "caseCategory"),

    CASE_ID("caseId", "caseId", "caseId"),
    CASE_ID_SNAKE_CASE("case_id", "caseId", "caseId"),

    CASE_NAME_CAMEL_CASE("caseName", "caseName", "caseName"),
    CASE_NAME_SNAKE_CASE("case_name", "caseName", "caseName"),

    NEXT_HEARING_DATE_CAMEL_CASE("nextHearingDate", "nextHearingDate", "nextHearingDate"),
    NEXT_HEARING_DATE_SNAKE_CASE("next_hearing_date", "nextHearingDate", "nextHearingDate");

    @JsonValue
    private final String id;
    private final String camundaVariableName;
    private final String cftVariableName;

    SortField(String id, String camundaVariableName, String cftVariableName) {
        this.id = id;
        this.camundaVariableName = camundaVariableName;
        this.cftVariableName = cftVariableName;
    }

    @Override
    public String toString() {
        return this.id;
    }
}
