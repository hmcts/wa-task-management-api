package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda;

public enum TaskState {
    UNCONFIGURED("unconfigured"),
    UNASSIGNED("unassigned"),
    CONFIGURED("configured"),
    ASSIGNED("assigned"),
    REFERRED("referred"),
    COMPLETED("completed"),
    CANCELLED("cancelled");


    private final String taskState;

    TaskState(String taskState) {
        this.taskState = taskState;
    }

    public String  getTaskState() {
        return this.taskState;
    }
}
