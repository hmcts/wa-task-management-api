package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum TaskState {

    UNCONFIGURED("UNCONFIGURED"),
    PENDING_AUTO_ASSIGN("PENDING_AUTO_ASSIGN"),
    ASSIGNED("ASSIGNED"),
    UNASSIGNED("UNASSIGNED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    TERMINATED("TERMINATED"),
    PENDING_RECONFIGURATION("PENDING_RECONFIGURATION");

    private String taskState;

    TaskState(String taskState) {
        this.taskState = taskState;
    }

    public String getTaskState() {
        return taskState;
    }

}
