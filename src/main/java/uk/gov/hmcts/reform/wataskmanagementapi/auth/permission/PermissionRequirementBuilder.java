package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class PermissionRequirementBuilder {
    public static final String NOT_INITIATED_ERROR = "Permission requirements should be initiated first";
    public static final String ALREADY_INITIATED_ERROR = "Permission requirements has already been initiated";
    public static final String NOT_JOINED_ERROR = "Set the join type before adding the next permission requirement";
    public static final String NULL_PERMISSION_TYPES_ERROR = "Permission types cannot be null";
    public static final String NULL_PERMISSION_JOIN_ERROR = "Permission join cannot be null";
    public static final String EMPTY_PERMISSION_TYPES_ERROR = "Permission types cannot be empty";

    private PermissionRequirements root;
    private PermissionRequirements currentPermissionRequirements;

    public static PermissionRequirementBuilder builder() {
        return new PermissionRequirementBuilder();
    }

    public PermissionRequirementBuilder initPermissionRequirement(List<PermissionTypes> permissionTypes,
                                                                 PermissionJoin permissionJoin) {
        if (root != null) {
            throw new IllegalStateException(ALREADY_INITIATED_ERROR);
        }

        validatePermissionRequirement(permissionTypes, permissionJoin);

        currentPermissionRequirements = new PermissionRequirements();
        root = currentPermissionRequirements;
        currentPermissionRequirements.setPermissionRequirement(new PermissionRequirement(permissionTypes,
                                                                                         permissionJoin));
        return this;
    }

    public PermissionRequirementBuilder joinPermissionRequirement(PermissionJoin permissionJoin) {
        if (root == null) {
            throw new IllegalStateException(NOT_INITIATED_ERROR);
        }

        currentPermissionRequirements.setPermissionJoinType(permissionJoin);
        return this;
    }

    public PermissionRequirementBuilder nextPermissionRequirement(List<PermissionTypes> permissionTypes,
                                                                  PermissionJoin permissionJoin) {
        if (root == null) {
            throw new IllegalStateException(NOT_INITIATED_ERROR);
        }
        if (currentPermissionRequirements.getPermissionJoin() == null) {
            throw new IllegalStateException(NOT_JOINED_ERROR);
        }

        validatePermissionRequirement(permissionTypes, permissionJoin);

        PermissionRequirements nextPermissionRequirements = new PermissionRequirements();
        nextPermissionRequirements.setPermissionRequirement(new PermissionRequirement(permissionTypes, permissionJoin));
        currentPermissionRequirements.setNextPermissionRequirements(nextPermissionRequirements);
        currentPermissionRequirements = nextPermissionRequirements;
        return this;
    }

    public PermissionRequirements build() {
        return root;
    }

    public PermissionRequirements buildSingleType(PermissionTypes type) {
        PermissionRequirements permissionRequirements = new PermissionRequirements();
        permissionRequirements.setPermissionRequirement(new PermissionRequirement(List.of(type), PermissionJoin.OR));
        return permissionRequirements;
    }

    public PermissionRequirements buildSingleRequirementWithAnd(PermissionTypes... types) {
        PermissionRequirements permissionRequirements = new PermissionRequirements();
        permissionRequirements.setPermissionRequirement(new PermissionRequirement(asList(types), PermissionJoin.AND));
        return permissionRequirements;
    }

    public PermissionRequirements buildSingleRequirementWithOr(PermissionTypes... types) {
        PermissionRequirements permissionRequirements = new PermissionRequirements();
        permissionRequirements.setPermissionRequirement(new PermissionRequirement(asList(types), PermissionJoin.OR));
        return permissionRequirements;
    }

    private void validatePermissionRequirement(List<PermissionTypes> permissionTypes,
                                               PermissionJoin permissionJoin) {
        requireNonNull(permissionTypes, NULL_PERMISSION_TYPES_ERROR);
        if (CollectionUtils.isEmpty(permissionTypes)) {
            throw new IllegalArgumentException(EMPTY_PERMISSION_TYPES_ERROR);
        }
        requireNonNull(permissionJoin, NULL_PERMISSION_JOIN_ERROR);
    }
}
