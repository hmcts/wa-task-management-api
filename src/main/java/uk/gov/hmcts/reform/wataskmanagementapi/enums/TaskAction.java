package uk.gov.hmcts.reform.wataskmanagementapi.enums;

import java.util.Optional;

import static java.util.Arrays.stream;

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

    public static Optional<TaskAction> from(String value) {
        return stream(values())
            .filter(v -> v.getValue().equalsIgnoreCase(value))
            .findFirst();
    }

    public String getValue() {
        return value;
    }
}
