package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class PermissionRequirement {
    private final List<PermissionTypes> permissionTypes;
    private final PermissionJoin permissionJoin;

    PermissionRequirement(List<PermissionTypes> permissionTypes, PermissionJoin permissionJoin) {
        requireNonNull(permissionTypes);
        requireNonNull(permissionJoin);
        this.permissionTypes = permissionTypes;
        this.permissionJoin = permissionJoin;
    }

    public List<PermissionTypes> getPermissionTypes() {
        return permissionTypes;
    }

    public PermissionJoin getPermissionJoin() {
        return permissionJoin;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PermissionRequirement that = (PermissionRequirement) other;
        return permissionTypes.equals(that.permissionTypes) && permissionJoin == that.permissionJoin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionTypes, permissionJoin);
    }
}
