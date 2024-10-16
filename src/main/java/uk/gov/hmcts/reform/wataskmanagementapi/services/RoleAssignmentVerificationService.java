package uk.gov.hmcts.reform.wataskmanagementapi.services;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_NOT_FOUND_ERROR;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class RoleAssignmentVerificationService {

    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CftQueryService cftQueryService;
    private final CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @Autowired
    public RoleAssignmentVerificationService(CFTTaskDatabaseService cftTaskDatabaseService,
                                             CftQueryService cftQueryService,
                                             CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDb) {
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.cftQueryService = cftQueryService;
        this.cftSensitiveTaskEventLogsDatabaseService = cftSensitiveTaskEventLogsDb;
    }

    public TaskResource verifyRoleAssignments(String taskId,
                                              List<RoleAssignment> roleAssignments,
                                              PermissionRequirements permissionsRequired) {

        return verifyRoleAssignments(taskId, roleAssignments, permissionsRequired, null);
    }

    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public TaskResource verifyRoleAssignments(String taskId,
                                              List<RoleAssignment> roleAssignments,
                                              PermissionRequirements permissionsRequired,
                                              ErrorMessages customErrorMessage) {
        Optional<String> optionalCaseId = cftTaskDatabaseService.findCaseId(taskId);
        if (optionalCaseId.isEmpty()) {
            throw new TaskNotFoundException(TASK_NOT_FOUND_ERROR);
        } else {
            String caseId = optionalCaseId.get();

            List<RoleAssignment> filteredRoleAssignments = roleAssignments.stream()
                .filter(ra -> !ra.getRoleType().equals(RoleType.CASE) || ra.getAttributes() != null
                    && ra.getAttributes().get("caseId") != null
                    && caseId.equals(ra.getAttributes().get("caseId")))
                .toList();

            Optional<TaskResource> optionalTaskResource = cftQueryService.getTask(
                taskId, filteredRoleAssignments, permissionsRequired
            );

            if (optionalTaskResource.isEmpty()) {
                ErrorMessages currentErrorMessage = ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
                if (customErrorMessage != null) {
                    currentErrorMessage = customErrorMessage;
                }

                cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(taskId,
                    roleAssignments,
                    currentErrorMessage);
                throw new RoleAssignmentVerificationException(currentErrorMessage);

            }
            return optionalTaskResource.get();
        }
    }

}
