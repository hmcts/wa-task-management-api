package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleType {
    CASE, ORGANISATION, @JsonEnumDefaultValue UNKNOWN
}
