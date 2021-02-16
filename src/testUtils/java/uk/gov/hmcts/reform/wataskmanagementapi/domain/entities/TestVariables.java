package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class TestVariables {

    private final String caseId;
    private final String taskId;
    private final String processInstanceId;

    public TestVariables(String caseId, String taskId, String processInstanceId) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(processInstanceId, "processInstanceId must not be null");
        this.caseId = caseId;
        this.taskId = taskId;
        this.processInstanceId = processInstanceId;
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
}
