package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum BusinessContext {

    CFT_TASK("CFT_TASK");

    private final String value;

    BusinessContext(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
