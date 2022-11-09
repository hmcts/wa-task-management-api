package uk.gov.hmcts.reform.wataskmanagementapi.enums;

public enum TaskAction {

    CLAIM("Claim"),
    UNCLAIM("Unclaim"),
    COMPLETED("Complete"),
    CONFIGURE("Configure"),
    AUTO_ASSIGN("AutoAssign"),
    AUTO_UNASSIGN("AutoUnassign"),
    AUTO_UNASSIGN_ASSIGN("AutoUnassignAssign");

    private final String value;

    TaskAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
