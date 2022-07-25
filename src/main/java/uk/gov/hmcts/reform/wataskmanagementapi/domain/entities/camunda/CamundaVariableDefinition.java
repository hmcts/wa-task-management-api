package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum CamundaVariableDefinition {
    APPEAL_TYPE("appealType"),
    AUTO_ASSIGNED("autoAssigned"),
    ASSIGNEE("assignee"),
    CASE_ID("caseId"),
    CASE_NAME("caseName"),
    CASE_TYPE_ID("caseTypeId"),
    EXECUTION_TYPE("executionType"),
    FORM_KEY("formKey"),
    JURISDICTION("jurisdiction"),
    LOCATION("location"),
    LOCATION_NAME("locationName"),
    REGION("region"),
    SECURITY_CLASSIFICATION("securityClassification"),
    TASK_ID("taskId"),
    TASK_NAME("name"),
    TASK_STATE("taskState"),
    TASK_SYSTEM("taskSystem"),
    TASK_TYPE("taskType"),
    TITLE("title"),
    HAS_WARNINGS("hasWarnings"),
    WARNING_LIST("warningList"),
    CFT_TASK_STATE("cftTaskState"),
    INITIATION_TIMESTAMP("initiationTimestamp"),
    WORK_TYPE("workType"),
    CASE_MANAGEMENT_CATEGORY("caseManagementCategory"),
    ROLE_CATEGORY("roleCategory"),
    DESCRIPTION("description"),
    ADDITIONAL_PROPERTIES("additionalProperties"),
    NEXT_HEARING_ID("nextHearingId"),
    NEXT_HEARING_DATE("nextHearingDate"),
    MINOR_PRIORITY("minorPriority"),
    MAJOR_PRIORITY("majorPriority"),
    PRIORITY_DATE("priorityDate");


    private final String value;

    CamundaVariableDefinition(String value) {
        this.value = value;
    }

    public static Optional<CamundaVariableDefinition> from(String value) {
        return stream(values())
            .filter(v -> v.value.equals(value))
            .findFirst();
    }

    public String value() {
        return value;
    }

}
