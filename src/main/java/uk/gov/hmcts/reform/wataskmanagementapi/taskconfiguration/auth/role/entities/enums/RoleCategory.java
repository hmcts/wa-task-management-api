package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.enums;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum RoleCategory {
    JUDICIAL, LEGAL_OPERATIONS, @JsonEnumDefaultValue UNKNOWN
}
