package uk.gov.hmcts.reform.wataskmanagementapi.enums;

public enum TaskAction {

    CLAIM("Claim"),
    UNCLAIM("Unclaim");


    private final String value;

    TaskAction(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
