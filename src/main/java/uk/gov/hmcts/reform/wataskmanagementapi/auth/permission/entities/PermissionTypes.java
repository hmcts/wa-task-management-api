package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum PermissionTypes {

    READ("read"),
    REFER("refer"),
    OWN("own"),
    MANAGE("manage"),
    EXECUTE("execute"),
    CANCEL("cancel"),
    COMPLETE("complete"),
    COMPLETE_OWN("completeOwn"),
    CANCEL_OWN("cancelOwn"),
    CLAIM("claim"),
    UNCLAIM("unclaim"),
    ASSIGN("assign"),
    UNASSIGN("unassign"),
    UNCLAIM_ASSIGN("unclaimAssign"),
    UNASSIGN_CLAIM("unassignClaim"),
    UNASSIGN_ASSIGN("unassignAssign");

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
