package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum CFTTaskState {

    UNCONFIGURED("UNCONFIGURED"),
    PENDING_AUTO_ASSIGN("PENDING_AUTO_ASSIGN"),
    ASSIGNED("ASSIGNED"),
    CONFIGURED("CONFIGURED"),
    UNASSIGNED("UNASSIGNED"),
    COMPLETED("COMPLETED"),
    CANCELLED("CANCELLED"),
    TERMINATED("TERMINATED"),
    PENDING_RECONFIGURATION("PENDING_RECONFIGURATION");

    private String value;


    CFTTaskState(String value) {
        this.value = value;
    }

    public static Optional<CFTTaskState> from(
        String value
    ) {
        return stream(values())
            .filter(v -> v.getValue().equalsIgnoreCase(value))
            .findFirst();
    }

    public String getValue() {
        return value;
    }

}
