package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@Component
public class PermissionCheckService {

    private final IdamService idamService;
    private final PermissionEvaluatorService permissionEvaluatorService;
    private final RoleAssignmentService roleAssignmentService;
    private final CamundaService camundaService;

    public PermissionCheckService(IdamService idamService,
                                  PermissionEvaluatorService permissionEvaluatorService,
                                  RoleAssignmentService roleAssignmentService,
                                  CamundaService camundaService) {
        this.idamService = idamService;
        this.permissionEvaluatorService = permissionEvaluatorService;
        this.roleAssignmentService = roleAssignmentService;
        this.camundaService = camundaService;
    }

    public boolean validate(String authToken, String taskId, String assigneeId) {
        String userId = idamService.getUserId(authToken);
        Map<String, CamundaVariable> variables = camundaService.performGetVariablesAction(taskId);
        return doValidation(authToken, assigneeId, userId, variables);
    }

    private boolean doValidation(String authToken,
                                 String assigneeId,
                                 String userId,
                                 Map<String, CamundaVariable> variables) {
        return checkUserPermissions(authToken, userId, variables)
            && checkAssigneePermissions(authToken, assigneeId, variables);
    }

    private boolean checkAssigneePermissions(String authToken,
                                             String assigneeId,
                                             Map<String, CamundaVariable> variables) {
        return checkUserRoleHasPermissions(
            authToken,
            assigneeId,
            variables,
            Arrays.asList(OWN, EXECUTE)
        );
    }

    private boolean checkUserPermissions(String authToken,
                                         String userId,
                                         Map<String, CamundaVariable> variables) {
        return checkUserRoleHasPermissions(
            authToken,
            userId,
            variables,
            Collections.singletonList(MANAGE)
        );
    }

    private boolean checkUserRoleHasPermissions(String authToken,
                                                String userId,
                                                Map<String, CamundaVariable> variables,
                                                List<PermissionTypes> permissionTypes) {
        List<Assignment> userAssignments = roleAssignmentService.getRolesForUser(userId, authToken);
        return permissionEvaluatorService.hasAccess(variables, userAssignments, permissionTypes);
    }
}
