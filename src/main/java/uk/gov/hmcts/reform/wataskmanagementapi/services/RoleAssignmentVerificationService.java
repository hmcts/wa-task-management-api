package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
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

    /**
     * Helper method to evaluate whether a user should have access to a task.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param taskId                the task id obtained from camunda.
     * @param accessControlResponse the access control response containing user's role assignment.
     * @param permissionsRequired   the permissions that are required by the endpoint.
     */
    public TaskResource roleAssignmentVerification(String taskId,
                                                    AccessControlResponse accessControlResponse,
                                                    List<PermissionTypes> permissionsRequired) {

        return roleAssignmentVerification(taskId, accessControlResponse, permissionsRequired, null);
    }

    /**
     * Helper method to evaluate whether a user should have access to a task supports custom error message.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param taskId                the task id obtained from camunda.
     * @param accessControlResponse the access control response containing user's role assignment.
     * @param permissionsRequired   the permissions that are required by the endpoint.
     * @param customErrorMessage    an error message to return on the exception
     */
    public TaskResource roleAssignmentVerification(String taskId,
                                                    AccessControlResponse accessControlResponse,
                                                    List<PermissionTypes> permissionsRequired,
                                                    ErrorMessages customErrorMessage) {
        Optional<TaskResource> optionalTaskResource = getTaskForRolesAndPermissionTypes(taskId, accessControlResponse, permissionsRequired);

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

    /**
     * Helper method to evaluate whether a user should have access to a task supports custom error message.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param taskId                the task id obtained from camunda.
     * @param accessControlResponse the access control response containing user's role assignment.
     * @param permissionsRequired   the permissions that are required by the endpoint.
     */
    public  Optional<TaskResource> getTaskForRolesAndPermissionTypes(String taskId,
                                                   AccessControlResponse accessControlResponse,
                                                   List<PermissionTypes> permissionsRequired) {
        return cftQueryService
            .getTask(taskId, accessControlResponse, permissionsRequired);
    }

    public void roleAssignmentVerification(Map<String, CamundaVariable> variables,
                                            List<RoleAssignment> roleAssignments,
                                            List<PermissionTypes> permissionsRequired) {
        roleAssignmentVerification(variables, roleAssignments, permissionsRequired, null);
    }

    /**
     * Helper method to evaluate whether a user should have access to a task supports custom error message.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param variables           the task variables obtained from camunda.
     * @param roleAssignments     the role assignments of the user.
     * @param permissionsRequired the permissions that are required by the endpoint.
     * @param customErrorMessage  the permissions that are required by the endpoint.
     */
    public void roleAssignmentVerification(Map<String, CamundaVariable> variables,
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

    /**
     * Helper method to evaluate whether a user should have access to a task.
     * This method also performs extra checks for hierarchy and assignee.
     * If the user does not have access it will throw a {@link RoleAssignmentVerificationException}
     *
     * @param currentAssignee     the IDAM id of the assigned user in the task.
     * @param userId              the IDAM userId of the user making the request.
     * @param variables           the task variables obtained from camunda.
     * @param roleAssignments     the role assignments of the user.
     * @param permissionsRequired the permissions that are required by the endpoint.
     */
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
