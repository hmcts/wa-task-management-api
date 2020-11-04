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

    public String value() {
        return value;
    }
}
