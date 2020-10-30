package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

import com.fasterxml.jackson.annotation.JsonValue;

import static java.util.Arrays.stream;

public enum PermissionTypes {

    READ("Read"),
    REFER("Refer"),
    OWN("Own"),
    MANAGE("Manage"),
    EXECUTE("Execute"),
    CANCEL("Cancel");

    @JsonValue
    private String value;

    PermissionTypes(String value) {
        this.value = value;
    }

    public static PermissionTypes from(String value) {
        return stream(values())
            .filter(v -> v.getValue().equals(value))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(value + " is an unsupported operator"));
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
