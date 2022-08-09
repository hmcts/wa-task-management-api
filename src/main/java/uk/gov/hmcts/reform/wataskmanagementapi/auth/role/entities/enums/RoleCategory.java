package uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleCategory {
    JUDICIAL, LEGAL_OPERATIONS, ADMIN, CTSC, @JsonEnumDefaultValue UNKNOWN
}
