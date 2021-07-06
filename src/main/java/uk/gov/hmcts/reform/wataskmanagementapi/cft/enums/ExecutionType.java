package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum ExecutionType {

    MANUAL("MANUAL"),
    BUILT_IN("BUILT_IN"),
    CASE_EVENT("CASE_EVENT");

    private String executionType;

    ExecutionType(String executionType) {
        this.executionType = executionType;
    }

    public String getExecutionType() {
        return executionType;
    }
}
