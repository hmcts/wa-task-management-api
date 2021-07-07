package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum ExecutionType {

    MANUAL("MANUAL"),
    BUILT_IN("BUILT_IN"),
    CASE_EVENT("CASE_EVENT");

    private final String value;

    ExecutionType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
