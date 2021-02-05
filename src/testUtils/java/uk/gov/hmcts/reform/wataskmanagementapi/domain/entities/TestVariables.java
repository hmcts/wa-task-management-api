package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Objects;

@EqualsAndHashCode
@ToString
public class TestVariables {

    private final String caseId;
    private final String taskId;

    public TestVariables(String caseId, String taskId) {
        Objects.requireNonNull(caseId, "caseId must not be null");
        Objects.requireNonNull(taskId, "taskId must not be null");
        this.caseId = caseId;
        this.taskId = taskId;
    }

    public String getCaseId() {
        return caseId;
    }

    public String getTaskId() {
        return taskId;
    }
}
