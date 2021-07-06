package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum TaskSystem {

    SELF("SELF"),
    CTSC("CTSC");

    private String taskStystem;

    TaskSystem(String taskStystem) {
        this.taskStystem = taskStystem;
    }

    public String getTaskStystem() {
        return taskStystem;
    }
}
