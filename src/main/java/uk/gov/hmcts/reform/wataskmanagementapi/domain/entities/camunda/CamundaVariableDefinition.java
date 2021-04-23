package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

public enum CamundaVariableDefinition {
    APPEAL_TYPE("appealType"),
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
    HAS_WARNINGS("hasWarnings");

    private final String value;

    CamundaVariableDefinition(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

}
