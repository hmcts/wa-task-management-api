package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;
import static java.util.stream.Collectors.toMap;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.buildTaskActionAttribute;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.setTaskActionAttributes;


@Slf4j
@Component
@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyMethods"})
public class TaskAutoAssignmentService {

    private final RoleAssignmentService roleAssignmentService;
    private final CftQueryService cftQueryService;
    private final IdamTokenGenerator idamTokenGenerator;


    public TaskAutoAssignmentService(RoleAssignmentService roleAssignmentService,
                                     CftQueryService cftQueryService,
            IdamTokenGenerator idamTokenGenerator) {
        this.roleAssignmentService = roleAssignmentService;
        this.cftQueryService = cftQueryService;
        this.idamTokenGenerator = idamTokenGenerator;
    }


    public TaskResource reAutoAssignCFTTask(TaskResource taskResource) {
        String initialAssignee = taskResource.getAssignee();
        CFTTaskState initialCftState = taskResource.getState();
        //if task is found and assigned
        if (StringUtils.isNotBlank(taskResource.getAssignee())) {

            //get role assignments for the user
            List<RoleAssignment> roleAssignmentsForUser =
                roleAssignmentService.getRolesByUserId(taskResource.getAssignee());

            //build and run the role assignment clause query with list of permissions required
            Optional<TaskResource> taskWithValidPermissions = cftQueryService
                .getTask(taskResource.getTaskId(), roleAssignmentsForUser, List.of(OWN, EXECUTE));

            //if existing user role assignments not have required permissions,then unassign and rerun autoassignment
            if (taskWithValidPermissions.isEmpty()) {
                taskResource.setAssignee(null);
                taskResource.setState(CFTTaskState.UNASSIGNED);
                TaskResource newTaskResource = autoAssignCFTTask(taskResource);
                updateTaskActionAttributes(newTaskResource, initialCftState, initialAssignee);
                return newTaskResource;
            }
            //same user is still valid - Configure Action

            updateTaskActionAttributes(taskResource, initialCftState, initialAssignee);
            return taskResource;
        }
        TaskResource newTaskResource = autoAssignCFTTask(taskResource);
        updateTaskActionAttributes(newTaskResource, initialCftState, initialAssignee);
        return newTaskResource;
    }

    public TaskResource performAutoAssignment(String taskId, TaskResource taskResource) {
        boolean isOldAssigneeValid = false;
        String initialAssignee = taskResource.getAssignee();
        CFTTaskState initialCftState = taskResource.getState();

        if (taskResource.getAssignee() != null) {
            log.info("Task '{}' had previous assignee, checking validity.", taskId);
            //Task had previous assignee
            isOldAssigneeValid =
                checkAssigneeIsStillValid(taskResource, taskResource.getAssignee());
        }
        TaskResource newTaskResource = taskResource;
        if (isOldAssigneeValid) {
            log.info("Task '{}' had previous assignee, and was valid, keeping assignee.", taskId);
            //Keep old assignee from skeleton task and change state
            newTaskResource.setState(CFTTaskState.ASSIGNED);
        } else {
            log.info("Task '{}' has an invalid assignee, unassign it before auto-assigning.", taskId);
            if (taskResource.getAssignee() != null) {
                newTaskResource.setAssignee(null);
                newTaskResource.setState(CFTTaskState.UNASSIGNED);
            }
            log.info("Task '{}' did not have previous assignee or was invalid, attempting to auto-assign.", taskId);
            //Otherwise attempt auto-assignment
            newTaskResource = autoAssignCFTTask(taskResource);
        }
        updateTaskActionAttributes(newTaskResource, initialCftState, initialAssignee);
        return newTaskResource;
    }

    public TaskResource autoAssignCFTTask(TaskResource taskResource) {
        List<RoleAssignment> roleAssignments =
            roleAssignmentService.queryRolesForAutoAssignmentByCaseId(taskResource);

        //The query above may return multiple role assignments, and we must select the right one for auto-assignment.
        //
        //    Sort the list of role assignments returned by assignment priority (from the task permissions data).
        //    For each role assignment in this sorted order, if the task role resource permissions data
        //    for the role name of this role assignment has:
        //    - An empty list of authorisations then ignore the role assignment.
        //    - Contain authorizations then check if role-assignment contains at least one of them.
        //
        //The first role assignment which is not ignored is the role assignment to be used for auto-assignment.

        if (roleAssignments.isEmpty() || taskResource.getTaskRoleResources() == null) {
            //already task action is set as Configure or AutoUnAssign
            taskResource.setAssignee(null);
            taskResource.setState(CFTTaskState.UNASSIGNED);
        } else {

            Optional<RoleAssignment> match = runRoleAssignmentAutoAssignVerification(taskResource, roleAssignments);

            if (match.isPresent()) {
                //The actorId of the role assignment selected in stage above is the IdAM ID of the user who is
                //to be assigned the task.
                taskResource.setAssignee(match.get().getActorId());
                taskResource.setState(CFTTaskState.ASSIGNED);
                taskResource.setAutoAssigned(true);
            } else {
                //If stage above produces an empty result of matching role assignment, then the task is
                //to be left unassigned.
                taskResource.setAssignee(null);
                taskResource.setState(CFTTaskState.UNASSIGNED);
            }
        }
        return taskResource;
    }

    public boolean checkAssigneeIsStillValid(TaskResource taskResource, String assignee) {

        List<RoleAssignment> roleAssignments = roleAssignmentService.getRolesByUserId(assignee);

        Optional<RoleAssignment> match = runRoleAssignmentAutoAssignVerification(taskResource, roleAssignments);
        return match.isPresent();
    }

    private Optional<RoleAssignment> runRoleAssignmentAutoAssignVerification(TaskResource taskResource,
                                                                             List<RoleAssignment> roleAssignments) {
        // the lowest assignment priority takes precedence.
        List<TaskRoleResource> rolesList = new ArrayList<>(taskResource.getTaskRoleResources());
        rolesList.sort(comparing(TaskRoleResource::getAssignmentPriority, nullsLast(Comparator.naturalOrder())));

        Map<String, TaskRoleResource> roleResourceMap = rolesList.stream()
            .collect(toMap(
                TaskRoleResource::getRoleName,
                taskRoleResource -> taskRoleResource,
                (a, b) -> b
            ));

        List<RoleAssignment> orderedRoleAssignments = orderRoleAssignments(rolesList, roleAssignments);
        return orderedRoleAssignments.stream()
            .filter(roleAssignment -> isRoleAssignmentValid(taskResource, roleResourceMap, roleAssignment)).findFirst();
    }

    private boolean isRoleAssignmentValid(TaskResource taskResource, Map<String,
        TaskRoleResource> roleResourceMap, RoleAssignment roleAssignment) {
        final TaskRoleResource taskRoleResource = roleResourceMap.get(roleAssignment.getRoleName());

        if (hasTaskBeenAssigned(taskResource, taskRoleResource)) {
            return findMatchingRoleAssignment(taskRoleResource, roleAssignment);
        } else if (isTaskRoleAutoAssignableWithNullOrEmptyAuthorisations(taskRoleResource)) {
            return true;
        } else if (isTaskRoleNotAutoAssignableOrAuthorisationsNotMatching(roleAssignment, taskRoleResource)) {
            return false;
        } else {
            return findMatchingRoleAssignment(taskRoleResource, roleAssignment);
        }
    }

    private boolean isTaskRoleNotAutoAssignableOrAuthorisationsNotMatching(RoleAssignment roleAssignment,
                                                                           TaskRoleResource taskRoleResource) {
        return !taskRoleResource.getAutoAssignable()
               || isAuthorisationsValid(taskRoleResource)
                  && !findMatchingRoleAssignment(taskRoleResource, roleAssignment);
    }

    private boolean isTaskRoleAutoAssignableWithNullOrEmptyAuthorisations(TaskRoleResource taskRoleResource) {
        return taskRoleResource.getAutoAssignable()
               && !isAuthorisationsValid(taskRoleResource);
    }

    private boolean hasTaskBeenAssigned(TaskResource taskResource, TaskRoleResource taskRoleResource) {
        return !taskRoleResource.getAutoAssignable()
               && taskResource.getAssignee() != null
               && isAuthorisationsValid(taskRoleResource);
    }

    private boolean isAuthorisationsValid(TaskRoleResource taskRoleResource) {
        return ArrayUtils.isNotEmpty(taskRoleResource.getAuthorizations());
    }

    private boolean findMatchingRoleAssignment(TaskRoleResource taskRoleResource, RoleAssignment roleAssignment) {
        AtomicBoolean hasMatch = new AtomicBoolean(false);
        Stream.of(taskRoleResource.getAuthorizations())
            .forEach(auth -> {
                //Safe-guard
                if (!hasMatch.get()
                    && roleAssignment.getAuthorisations() != null
                    && roleAssignment.getAuthorisations().contains(auth)) {
                    hasMatch.set(true);
                }
            });
        return hasMatch.get();
    }

    private List<RoleAssignment> orderRoleAssignments(List<TaskRoleResource> rolesList,
                                                      List<RoleAssignment> roleAssignments) {
        List<RoleAssignment> orderedRoleAssignments = new ArrayList<>();
        rolesList.forEach(role -> {
            List<RoleAssignment> filtered = roleAssignments.stream()
                .filter(ra -> role.getRoleName().equals(ra.getRoleName()))
                .collect(Collectors.toList());
            orderedRoleAssignments.addAll(filtered);
        });
        return orderedRoleAssignments;
    }

    private void updateTaskActionAttributes(TaskResource taskResource, CFTTaskState previousCftTaskState,
                                            String previousAssignee) {
        String systemUserToken = idamTokenGenerator.generate();
        String systemUserId = idamTokenGenerator.getUserInfo(systemUserToken).getUid();
        TaskAction taskAction = buildTaskActionAttribute(taskResource, previousCftTaskState, previousAssignee);
        setTaskActionAttributes(taskResource, systemUserId, taskAction);
    }

}
