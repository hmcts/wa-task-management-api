package uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode
@ToString
public class AutoAssignmentResult {
    private final String taskState;
    private final String assignee;

    public AutoAssignmentResult(String taskState, String assignee) {
        this.taskState = taskState;
        this.assignee = assignee;
    }

    public String getTaskState() {
        return taskState;
    }

    public String getAssignee() {
        return assignee;
    }
}
