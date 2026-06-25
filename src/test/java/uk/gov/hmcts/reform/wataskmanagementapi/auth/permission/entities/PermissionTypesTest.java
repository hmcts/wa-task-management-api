package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PermissionTypesTest {

    @ParameterizedTest
    @EnumSource(PermissionTypes.class)
    void should_deserialise_permission_type_from_api_value(PermissionTypes permissionType) {
        assertEquals(permissionType, PermissionTypes.fromJson(permissionType.value()));
    }

    @Test
    void should_map_permission_specific_task_role_resource_field() {
        assertEquals("completeOwn", PermissionTypes.COMPLETE_OWN.taskRoleResourceField());
        assertEquals("unclaimAssign", PermissionTypes.UNCLAIM_ASSIGN.taskRoleResourceField());
    }

    @Test
    void should_reject_unknown_permission_type() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PermissionTypes.fromJson("Unknown")
        );

        assertEquals("Unknown PermissionTypes: Unknown", exception.getMessage());
    }
}
