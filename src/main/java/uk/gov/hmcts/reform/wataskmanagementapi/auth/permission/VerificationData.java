package uk.gov.hmcts.reform.wataskmanagementapi.auth.permission;

import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class VerificationData {

    private final Map<String, CamundaVariable> taskVariables;
    private final Assignment roleAssignment;
    private final List<PermissionTypes> permissionsRequired;

    public VerificationData(Map<String, CamundaVariable> taskVariables,
                            Assignment roleAssignment,
                            List<PermissionTypes> permissionsRequired) {

        Objects.requireNonNull(taskVariables, "Task Variables Map cannot be null.");
        Objects.requireNonNull(roleAssignment, "Role Assignment cannot be null.");
        Objects.requireNonNull(permissionsRequired, "Operation Permissions cannot be null.");

        this.taskVariables = taskVariables;
        this.roleAssignment = roleAssignment;
        this.permissionsRequired = permissionsRequired;
    }

    public Map<String, CamundaVariable> getTaskVariables() {
        return taskVariables;
    }

    public Assignment getRoleAssignment() {
        return roleAssignment;
    }

    public List<PermissionTypes> getPermissionsRequired() {
        return permissionsRequired;
    }
}
