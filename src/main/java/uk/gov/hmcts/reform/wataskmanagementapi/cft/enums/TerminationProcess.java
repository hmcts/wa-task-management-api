package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import lombok.Getter;

@Getter
public enum TerminationProcess {

    EXUI_USER_COMPLETION("EXUI_USER_COMPLETION"),
    EXUI_CASE_EVENT_COMPLETION("EXUI_CASE-EVENT_COMPLETION");

    private final String value;


    TerminationProcess(String value) {
        this.value = value;
    }
}
