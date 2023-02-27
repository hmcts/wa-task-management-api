package uk.gov.hmcts.reform.wataskmanagementapi.domain;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.WarningValues;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class TestVariables {

    private final String caseId;
    private final String taskId;
    private final String taskName;
    private final String processInstanceId;
    private final String taskType;
    private final WarningValues warnings;

    public TestVariables(String caseId, String taskId, String processInstanceId, String taskType,
                         String taskName, WarningValues warnings) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(taskName, "taskName must not be null");
        Objects.requireNonNull(processInstanceId, "processInstanceId must not be null");
        Objects.requireNonNull(taskType, "taskType must not be null");
        Objects.requireNonNull(warnings, "warnings must not be null");
        this.caseId = caseId;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
        this.taskType = taskType;
        this.taskName = taskName;
        this.warnings = warnings;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public String getTaskType() {
        return taskType;
    }

    public String getTaskName() {
        return taskName;
    }

    public WarningValues getWarnings() {
        return warnings;
    }
}
