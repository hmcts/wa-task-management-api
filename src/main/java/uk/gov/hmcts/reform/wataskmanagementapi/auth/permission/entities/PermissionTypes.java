package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

import static java.util.Arrays.stream;

public enum PermissionTypes {

    READ("Read", "read"),
    REFER("Refer", "refer"),
    OWN("Own", "own"),
    MANAGE("Manage", "manage"),
    EXECUTE("Execute", "execute"),
    CANCEL("Cancel", "cancel"),
    COMPLETE("Complete", "complete"),
    COMPLETE_OWN("CompleteOwn", "completeOwn"),
    CANCEL_OWN("CancelOwn", "cancelOwn"),
    CLAIM("Claim", "claim"),
    UNCLAIM("Unclaim", "unclaim"),
    ASSIGN("Assign", "assign"),
    UNASSIGN("Unassign", "unassign"),
    UNCLAIM_ASSIGN("UnclaimAssign", "unclaimAssign"),
    UNASSIGN_CLAIM("UnassignClaim", "unassignClaim"),
    UNASSIGN_ASSIGN("UnassignAssign","unassignAssign");

    @JsonValue
    private String value;

    private String mapperValue;

    PermissionTypes(String value, String mapperValue) {
        this.value = value;
        this.mapperValue = mapperValue;
    }

    public static Optional<PermissionTypes> from(String value) {
        return stream(values())
            .filter(v -> v.value.equals(value))
            .findFirst();
    }

    public String value() {
        return value;
    }

    public String getMapperValue() {
        return mapperValue;
    }

}
