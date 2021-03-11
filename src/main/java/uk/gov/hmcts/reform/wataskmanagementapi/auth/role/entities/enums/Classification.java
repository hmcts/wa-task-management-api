package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum Classification {
    PUBLIC, PRIVATE, RESTRICTED, @JsonEnumDefaultValue UNKNOWN
}
