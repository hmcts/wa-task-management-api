package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_NOT_FOUND_ERROR;

@Slf4j
@Component
public class RoleAssignmentVerificationService {

    private final PermissionEvaluatorService permissionEvaluatorService;
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CftQueryService cftQueryService;

    @Autowired
    public RoleAssignmentVerificationService(PermissionEvaluatorService permissionEvaluatorService,
                                             CFTTaskDatabaseService cftTaskDatabaseService,
                                             CftQueryService cftQueryService) {
        this.permissionEvaluatorService = permissionEvaluatorService;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.cftQueryService = cftQueryService;
    }

    public TaskResource verifyRoleAssignments(String taskId,
                                              List<RoleAssignment> roleAssignments,
                                              PermissionRequirements permissionsRequired) {

        return verifyRoleAssignments(taskId, roleAssignments, permissionsRequired, null);
    }

    public TaskResource verifyRoleAssignments(String taskId,
                                              List<RoleAssignment> roleAssignments,
                                              PermissionRequirements permissionsRequired,
                                              ErrorMessages customErrorMessage) {
        Optional<TaskResource> optionalTaskResource = cftQueryService.getTask(
            taskId, roleAssignments, permissionsRequired
        );

        if (optionalTaskResource.isEmpty()) {
            Optional<TaskResource> optionalTask = cftTaskDatabaseService.findByIdOnly(taskId);
            if (optionalTask.isEmpty()) {
                throw new TaskNotFoundException(TASK_NOT_FOUND_ERROR);
            } else {
                if (customErrorMessage != null) {
                    throw new RoleAssignmentVerificationException(customErrorMessage);
                }
                throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
            }
        }
        return optionalTaskResource.get();
    }

    public void verifyRoleAssignments(Map<String, CamundaVariable> variables,
                                      List<RoleAssignment> roleAssignments,
                                      List<PermissionTypes> permissionsRequired) {
        verifyRoleAssignments(variables, roleAssignments, permissionsRequired, null);
    }

    public void verifyRoleAssignments(Map<String, CamundaVariable> variables,
                                      List<RoleAssignment> roleAssignments,
                                      List<PermissionTypes> permissionsRequired,
                                      ErrorMessages customErrorMessage) {
        boolean hasAccess = permissionEvaluatorService.hasAccess(variables, roleAssignments, permissionsRequired);
        if (!hasAccess) {
            if (customErrorMessage != null) {
                throw new RoleAssignmentVerificationException(customErrorMessage);
            }
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }
    }

    public void roleAssignmentVerificationWithAssigneeCheckAndHierarchy(String currentAssignee,
                                                                        String userId,
                                                                        Map<String, CamundaVariable> variables,
                                                                        List<RoleAssignment> roleAssignments,
                                                                        List<PermissionTypes> permissionsRequired) {
        boolean hasAccess = permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            currentAssignee,
            userId,
            variables,
            roleAssignments,
            permissionsRequired
        );
        if (!hasAccess) {
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }
    }

}
