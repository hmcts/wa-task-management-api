package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Classification {
    PUBLIC("U"), PRIVATE("P"), RESTRICTED("R"), @JsonEnumDefaultValue UNKNOWN(null);

    private final String abbreviation;

    Classification(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
