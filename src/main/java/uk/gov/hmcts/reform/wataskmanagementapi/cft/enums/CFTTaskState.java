package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum CFTTaskState {

    UNCONFIGURED("UNCONFIGURED"),
    PENDING_AUTO_ASSIGN("PENDING_AUTO_ASSIGN"),
    ASSIGNED("ASSIGNED"),
    UNASSIGNED("UNASSIGNED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    TERMINATED("TERMINATED"),
    PENDING_RECONFIGURATION("PENDING_RECONFIGURATION");

    private String value;

    CFTTaskState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
