package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

public enum TerminationProcess {

    EXUI_USER_COMPLETION("EXUI_USER_COMPLETION"),
    EXUI_CASE_EVENT_COMPLETION("EXUI_CASE-EVENT_COMPLETION");

    private String value;

    TerminationProcess(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
