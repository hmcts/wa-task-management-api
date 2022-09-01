package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum PermissionTypes {

    READ("Read"),
    REFER("Refer"),
    OWN("Own"),
    MANAGE("Manage"),
    EXECUTE("Execute"),
    CANCEL("Cancel"),
    COMPLETE("Complete"),
    COMPLETE_OWN("CompleteOwn"),
    CANCEL_OWN("CancelOwn"),
    CLAIM("Claim"),
    UNCLAIM("Unclaim"),
    ASSIGN("Assign"),
    UNASSIGN("Unassign"),
    UNCLAIM_ASSIGN("UnclaimAssign"),
    UNASSIGN_CLAIM("UnassignClaim"),
    UNASSIGN_ASSIGN("UnassignAssign");

    @JsonValue
    private String value;

    PermissionTypes(String value) {
        this.value = value;
    }

    public static Optional<PermissionTypes> from(String value) {
        return stream(values())
            .filter(v -> v.value.equals(value))
            .findFirst();
    }

    public String value() {
        return value;
    }
}
