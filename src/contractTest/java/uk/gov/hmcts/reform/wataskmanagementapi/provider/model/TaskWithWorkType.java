package uk.gov.hmcts.reform.wataskmanagementapi.provider.model;

import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;

import java.time.ZonedDateTime;

public class TaskWithWorkType extends uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task {
    private WorkType workType;

    public TaskWithWorkType(String id, String name, String type, String taskState,
                            String taskSystem, String securityClassification, String taskTitle,
                            ZonedDateTime createdDate, ZonedDateTime dueDate, String assignee,
                            boolean autoAssigned, String executionType, String jurisdiction,
                            String region, String location, String locationName,
                            String caseTypeId, String caseId, String caseCategory,
                            String caseName, Boolean hasWarnings, WarningValues warningList,
                            String caseManagementCategory, WorkType workType) {
        super(id, name, type, taskState, taskSystem, securityClassification,
            taskTitle, createdDate, dueDate, assignee, autoAssigned,
            executionType, jurisdiction, region, location, locationName,
            caseTypeId, caseId, caseCategory, caseName, hasWarnings,
            warningList, caseManagementCategory);
        this.workType = workType;
    }
}
