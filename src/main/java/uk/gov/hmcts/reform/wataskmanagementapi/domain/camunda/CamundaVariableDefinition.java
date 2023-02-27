package uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda;

import com.fasterxml.jackson.core.type.TypeReference;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.stream;

public enum CamundaVariableDefinition {
    APPEAL_TYPE("appealType", new TypeReference<String>() {}),
    AUTO_ASSIGNED("autoAssigned", new TypeReference<Boolean>() {}),
    ASSIGNEE("assignee", new TypeReference<String>() {}),
    CASE_ID("caseId", new TypeReference<String>() {}),
    CASE_NAME("caseName", new TypeReference<String>() {}),
    CASE_TYPE_ID("caseTypeId", new TypeReference<String>() {}),
    EXECUTION_TYPE("executionType", new TypeReference<String>() {}),
    FORM_KEY("formKey", new TypeReference<String>() {}),
    JURISDICTION("jurisdiction", new TypeReference<String>() {}),
    LOCATION("location", new TypeReference<String>() {}),
    LOCATION_NAME("locationName", new TypeReference<String>() {}),
    REGION("region", new TypeReference<String>() {}),
    SECURITY_CLASSIFICATION("securityClassification", new TypeReference<SecurityClassification>() {}),
    ASSIGNMENT_EXPIRY("assignmentExpiry", new TypeReference<String>() {}),
    REGION_NAME("regionName", new TypeReference<String>() {}),
    TERMINATION_REASON("terminationReason", new TypeReference<String>() {}),
    BUSINESS_CONTEXT("businessContext", new TypeReference<String>() {}),
    CASE_CATEGORY("caseCategory", new TypeReference<String>() {}),
    TASK_ROLES("task_roles", new TypeReference<Set<String>>() {}),
    TASK_ID("taskId", new TypeReference<String>() {}),
    TASK_NAME("name", new TypeReference<String>() {}),
    DUE_DATE("dueDate", new TypeReference<String>() {}),
    CREATED("created", new TypeReference<String>() {}),
    TASK_STATE("taskState", new TypeReference<CFTTaskState>() {}),
    TASK_SYSTEM("taskSystem", new TypeReference<TaskSystem>() {}),
    TASK_TYPE("taskType", new TypeReference<String>() {}),
    TITLE("title", new TypeReference<String>() {}),
    HAS_WARNINGS("hasWarnings", new TypeReference<Boolean>() {}),
    WARNING_LIST("warningList", new TypeReference<WarningValues>() {}),
    CFT_TASK_STATE("cftTaskState", new TypeReference<String>() {}),
    INITIATION_TIMESTAMP("initiationTimestamp", new TypeReference<String>() {}),
    WORK_TYPE("workType", new TypeReference<String>() {}),
    CASE_MANAGEMENT_CATEGORY("caseManagementCategory", new TypeReference<String>() {}),
    ROLE_CATEGORY("roleCategory", new TypeReference<String>() {}),
    DESCRIPTION("description", new TypeReference<String>() {}),
    ADDITIONAL_PROPERTIES("additionalProperties", new TypeReference<Map<String, String>>() {}),
    NEXT_HEARING_ID("nextHearingId", new TypeReference<String>() {}),
    NEXT_HEARING_DATE("nextHearingDate", new TypeReference<String>() {}),
    MINOR_PRIORITY("minorPriority", new TypeReference<Integer>() {}),
    MAJOR_PRIORITY("majorPriority", new TypeReference<Integer>() {}),
    PRIORITY_DATE("priorityDate", new TypeReference<String>() {}),
    ROLE_ASSIGNMENT_ID("roleAssignmentId", new TypeReference<String>() {}),
    NOTES("notes", new TypeReference<String>() {}),
    ROLES("roles", new TypeReference<String>() {});


    private final String value;
    private final TypeReference typeReference;

    CamundaVariableDefinition(String value, TypeReference typeReference) {
        this.value = value;
        this.typeReference = typeReference;
    }

    public static Optional<CamundaVariableDefinition> from(String value) {
        return stream(values())
            .filter(v -> v.value.equals(value))
            .findFirst();
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }

    public String value() {
        return value;
    }

}
