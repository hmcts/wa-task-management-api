package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum TaskSystem {

    SELF("SELF"),
    CTSC("CTSC");

    private String value;

    TaskSystem(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
