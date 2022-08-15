package uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums;

import com.fasterxml.jackson.core.type.TypeReference;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("java:S3740")
public enum TaskAttributeDefinition {
    TASK_ASSIGNEE("task_assignee", new TypeReference<String>() {}),
    TASK_ASSIGNMENT_EXPIRY("task_assignment_expiry", new TypeReference<String>() {}),
    TASK_AUTO_ASSIGNED("task_auto_assigned", new TypeReference<Boolean>() {}),
    TASK_BUSINESS_CONTEXT("task_business_context", new TypeReference<String>() {}),
    TASK_CASE_ID("task_case_id", new TypeReference<String>() {}),
    TASK_CASE_NAME("task_case_name", new TypeReference<String>() {}),
    TASK_CASE_TYPE_ID("task_case_type_id", new TypeReference<String>() {}),
    TASK_CASE_CATEGORY("task_case_category", new TypeReference<String>() {}),
    TASK_CREATED("task_created", new TypeReference<String>() {}),
    TASK_DESCRIPTION("task_description", new TypeReference<String>() {}),
    TASK_DUE_DATE("task_due_date", new TypeReference<String>() {}),
    TASK_EXECUTION_TYPE_NAME("task_execution_type_name", new TypeReference<String>() {}),
    TASK_HAS_WARNINGS("task_has_warnings", new TypeReference<Boolean>() {}),
    TASK_JURISDICTION("task_jurisdiction", new TypeReference<String>() {}),
    TASK_LOCATION("task_location", new TypeReference<String>() {}),
    TASK_LOCATION_NAME("task_location_name", new TypeReference<String>() {}),
    TASK_MAJOR_PRIORITY("task_major_priority", new TypeReference<Integer>() {}),
    TASK_MINOR_PRIORITY("task_minor_priority", new TypeReference<Integer>() {}),
    TASK_NAME("task_name", new TypeReference<String>() {}),
    TASK_WARNINGS("task_warnings", new TypeReference<WarningValues>() {}),
    TASK_NOTES("task_notes", new TypeReference<String>() {}),
    TASK_REGION("task_region", new TypeReference<String>() {}),
    TASK_REGION_NAME("task_region_name", new TypeReference<String>() {}),
    TASK_ROLE_CATEGORY("task_role_category", new TypeReference<String>() {}),
    TASK_ROLES("task_roles", new TypeReference<Set<String>>() {}),
    TASK_SECURITY_CLASSIFICATION("task_security_classification", new TypeReference<SecurityClassification>() {}),
    TASK_STATE("task_state", new TypeReference<CFTTaskState>() {}),
    TASK_SYSTEM("task_system", new TypeReference<TaskSystem>() {}),
    TASK_TERMINATION_REASON("task_termination_reason", new TypeReference<String>() {}),
    TASK_TITLE("task_title", new TypeReference<String>() {}),
    TASK_TYPE("task_type", new TypeReference<String>() {}),
    TASK_WORK_TYPE("task_work_type", new TypeReference<String>() {}),
    TASK_ADDITIONAL_PROPERTIES("task_additional_properties", new TypeReference<Map<String, String>>() {}),
    TASK_NEXT_HEARING_ID("task_next_hearing_id", new TypeReference<String>() {}),
    TASK_NEXT_HEARING_DATE("task_next_hearing_date", new TypeReference<String>() {}),
    TASK_PRIORITY_DATE("task_priority_date", new TypeReference<String>() {});



    private final String value;
    private final TypeReference typeReference;

    TaskAttributeDefinition(String value, TypeReference typeReference) {
        this.value = value;
        this.typeReference = typeReference;
    }

    public String value() {
        return value;
    }

    public TypeReference getTypeReference() {
        return typeReference;
    }
}
