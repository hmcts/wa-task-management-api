package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums;

public enum CamundaVariableDefinition {

    APPEAL_TYPE("appealType"),
    AUTO_ASSIGNED("autoAssigned"),
    CASE_NAME("caseName"),
    CASE_TYPE_ID("caseTypeId"),
    EXECUTION_TYPE("executionType"),
    LOCATION("location"),
    LOCATION_NAME("locationName"),
    REGION("region"),
    SECURITY_CLASSIFICATION("securityClassification"),
    TASK_STATE("taskState"),
    TASK_SYSTEM("taskSystem"),
    TITLE("title"),
    JURISDICTION("jurisdiction"),
    ASSIGNEE("assignee"),
    TASK_ID("taskId"),
    TASK_TYPE("taskType"),
    FORM_KEY("formKey"),
    CASE_ID("caseId"),
    NAME("name"),
    HAS_WARNINGS("hasWarnings");

    private final String value;

    CamundaVariableDefinition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
