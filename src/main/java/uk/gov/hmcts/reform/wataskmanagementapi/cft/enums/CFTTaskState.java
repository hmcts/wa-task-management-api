package uk.gov.hmcts.reform.wataskmanagementapi.cft.enums;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum CFTTaskState {

    UNCONFIGURED("UNCONFIGURED", "UCNF"),
    PENDING_AUTO_ASSIGN("PENDING_AUTO_ASSIGN", "PA"),
    ASSIGNED("ASSIGNED", "A"),
    CONFIGURED("CONFIGURED", "CNF"),
    UNASSIGNED("UNASSIGNED", "U"),
    COMPLETED("COMPLETED", "C"),
    CANCELLED("CANCELLED", "CAN"),
    TERMINATED("TERMINATED", "T"),
    PENDING_RECONFIGURATION("PENDING_RECONFIGURATION", "PR");

    private String value;

    private String abbreviation;


    CFTTaskState(String value, String abbreviation) {
        this.value = value;
        this.abbreviation = abbreviation;
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

    public String getAbbreviation() {
        return abbreviation;
    }

}
