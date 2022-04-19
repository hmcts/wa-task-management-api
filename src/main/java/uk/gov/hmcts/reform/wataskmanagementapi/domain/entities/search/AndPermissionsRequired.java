package uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

/*
Represents a list of permissions to be used for searching which should be OR'd together.
Used for SearchQuery building.
E.g. READ AND OWN
 */
public final class AndPermissionsRequired implements SearchPermissionsRequired {

    private final List<PermissionTypes> permissions;

    public AndPermissionsRequired(List<PermissionTypes> permissions) {
        this.permissions = permissions;
    }

    @Override
    public List<PermissionTypes> getPermissions() {
        return permissions;
    }
}
