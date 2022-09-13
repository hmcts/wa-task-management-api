package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;


import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;

import java.util.Objects;


public class PermissionRequirements {

    private PermissionRequirement permissionRequirement;
    private PermissionJoin permissionJoin;
    private PermissionRequirements nextPermissionRequirements;


    PermissionRequirements() {
        //Restricted constructor only accessible to the PermissionRequirementBuilder
    }

    void setPermissionRequirement(PermissionRequirement permissionRequirement) {
        this.permissionRequirement = permissionRequirement;
    }

    void setPermissionJoinType(PermissionJoin permissionJoin) {
        this.permissionJoin = permissionJoin;
    }

    void setNextPermissionRequirements(PermissionRequirements nextPermissionRequirements) {
        this.nextPermissionRequirements = nextPermissionRequirements;
    }

    public PermissionRequirement getPermissionRequirement() {
        return permissionRequirement;
    }

    public PermissionJoin getPermissionJoin() {
        return permissionJoin;
    }

    public PermissionRequirements getNextPermissionRequirements() {
        return nextPermissionRequirements;
    }

    public boolean isEmpty() {
        return permissionRequirement == null
            || permissionRequirement.getPermissionTypes() == null
            || permissionRequirement.getPermissionTypes().isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        PermissionRequirements that = (PermissionRequirements) other;
        return Objects.equals(permissionRequirement, that.permissionRequirement)
            && permissionJoin == that.permissionJoin
            && Objects.equals(nextPermissionRequirements, that.nextPermissionRequirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(permissionRequirement, permissionJoin, nextPermissionRequirements);
    }
}
