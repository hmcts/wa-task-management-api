package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleCategory {
    JUDICIAL("J"),
    LEGAL_OPERATIONS("L"),
    ADMIN("A"),
    CTSC("C"),
    @JsonEnumDefaultValue UNKNOWN("U");

    private String abbreviation;

    RoleCategory(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
