package uk.gov.hmcts.reform.wataskmanagementapi.cft.entities;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class TaskResourceSummary {

    private String taskId;
    private OffsetDateTime dueDateTime;
    private String caseId;
    private String caseName;
    private String caseCategory;
    private String locationName;
    private String title;

    public TaskResourceSummary(String taskId, OffsetDateTime dueDateTime, String caseId, String caseName,
                               String caseCategory, String locationName, String title) {
        this.taskId = taskId;
        this.dueDateTime = dueDateTime;
        this.caseId = caseId;
        this.caseName = caseName;
        this.caseCategory = caseCategory;
        this.locationName = locationName;
        this.title = title;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setDueDateTime(OffsetDateTime dueDateTime) {
        this.dueDateTime = dueDateTime;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public void setCaseCategory(String caseCategory) {
        this.caseCategory = caseCategory;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
