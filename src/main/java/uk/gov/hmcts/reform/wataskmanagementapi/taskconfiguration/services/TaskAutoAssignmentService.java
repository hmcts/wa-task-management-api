package uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.AutoAssignmentResult;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;

import java.util.List;

import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.enums.TaskState.UNASSIGNED;

@Slf4j
@Component
public class TaskAutoAssignmentService {

    private final RoleAssignmentService roleAssignmentService;
    private final CamundaService camundaService;

    public TaskAutoAssignmentService(RoleAssignmentService roleAssignmentService,
                                     CamundaService camundaService) {
        this.roleAssignmentService = roleAssignmentService;
        this.camundaService = camundaService;
    }

    public void autoAssignTask(TaskToConfigure taskToConfigure, String currentTaskState) {
        updateTaskStateAndSetAssignee(taskToConfigure, currentTaskState);
    }

    public AutoAssignmentResult getAutoAssignmentVariables(TaskToConfigure task) {
        List<RoleAssignment> roleAssignments = roleAssignmentService.searchRolesByCaseId(task.getCaseId());

        if (roleAssignments.isEmpty()) {
            // the user did not have specific role assignment for this case
            log.info("The case did not have specific users assigned, Setting task state to '{}'", UNASSIGNED);
            return new AutoAssignmentResult(UNASSIGNED.value(), null);
        } else {
            log.info("The case contained specific users assigned, Setting task state to '{}'", ASSIGNED);
            return new AutoAssignmentResult(ASSIGNED.value(), roleAssignments.get(0).getActorId());
        }
    }

    @SuppressWarnings({"PMD.LawOfDemeter"})
    private void updateTaskStateAndSetAssignee(TaskToConfigure taskToConfigure,
                                               String currentTaskState) {

        List<RoleAssignment> roleAssignments = roleAssignmentService.searchRolesByCaseId(taskToConfigure.getCaseId());
        log.info("Role assignments retrieved for caseId '{}'", taskToConfigure.getCaseId());
        if (roleAssignments.isEmpty()) {
            log.info("The case did not have specific users assigned, Setting task state to '{}'", UNASSIGNED);
            camundaService.updateTaskStateTo(taskToConfigure.getId(), UNASSIGNED);
        } else {
            String assignee = roleAssignments.get(0).getActorId();
            log.info(
                "The case contained specific users assigned, Setting task state to '{}' ",
                ASSIGNED
            );

            camundaService.assignTask(
                taskToConfigure.getId(),
                assignee,
                currentTaskState
            );

        }
    }

}
