package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.SelectTaskResourceQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskSearchQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestAttributes;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.AND;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.OR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ROLE_ASSIGNMENT_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_NOT_FOUND_ERROR;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.DataflowAnomalyAnalysis",
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveParameterList",
    "PMD.ExcessiveClassLength",
    "PMD.GodClass"})
public class TaskManagementService {
    public static final String USER_ID_CANNOT_BE_NULL = "UserId cannot be null";

    private final CamundaService camundaService;
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CFTTaskMapper cftTaskMapper;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;
    private final List<TaskOperationService> taskOperationServices;
    private final RoleAssignmentVerificationService roleAssignmentVerification;

    @PersistenceContext
    private final EntityManager entityManager;

    @Autowired
    public TaskManagementService(CamundaService camundaService,
                                 CFTTaskDatabaseService cftTaskDatabaseService,
                                 CFTTaskMapper cftTaskMapper,
                                 LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                 ConfigureTaskService configureTaskService,
                                 TaskAutoAssignmentService taskAutoAssignmentService,
                                 RoleAssignmentVerificationService roleAssignmentVerification,
                                 List<TaskOperationService> taskOperationServices,
                                 EntityManager entityManager) {
        this.camundaService = camundaService;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.cftTaskMapper = cftTaskMapper;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
        this.taskOperationServices = taskOperationServices;
        this.roleAssignmentVerification = roleAssignmentVerification;
        this.entityManager = entityManager;
    }

    /**
     * Retrieves a task from camunda, performs role assignment verifications and returns a mapped task.
     * This method requires {@link PermissionTypes#READ} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     * @return A mapped task {@link Task}
     */
    public Task getTask(String taskId, AccessControlResponse accessControlResponse) {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder().buildSingleType(READ);

        TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        Set<PermissionTypes> permissionsUnionForUser =
            cftTaskMapper.extractUnionOfPermissionsForUser(
                taskResource.getTaskRoleResources(),
                accessControlResponse.getRoleAssignments()
            );

        return cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnionForUser);
    }


    /**
     * Claims a task in camunda also performs role assignment verifications.
     * This method requires {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @Transactional
    public void claimTask(String taskId,
                          AccessControlResponse accessControlResponse) {
        String userId = accessControlResponse.getUserInfo().getUid();
        requireNonNull(userId, USER_ID_CANNOT_BE_NULL);
        String email = accessControlResponse.getUserInfo().getEmail();

        PermissionRequirements permissionsRequired;
        if (isGranularPermissionFeatureEnabled(userId, email)) {
            permissionsRequired = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(CLAIM, OWN), AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(asList(CLAIM, EXECUTE), AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(asList(ASSIGN, EXECUTE), AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(asList(ASSIGN, OWN), AND)
                .build();
        } else {
            permissionsRequired = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
        }

        roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        if (task.getState() == CFTTaskState.ASSIGNED && !task.getAssignee().equals(userId)) {
            throw new ConflictException("Task '" + task.getTaskId()
                                        + "' is already claimed by someone else.", null);
        }
        task.setState(CFTTaskState.ASSIGNED);
        task.setAssignee(userId);

        camundaService.assignTask(taskId, userId, false);

        //Commit transaction
        cftTaskDatabaseService.saveTask(task);
    }

    /**
     * Unclaims a task in camunda also performs role assignment verifications.
     * This method requires {@link PermissionTypes#MANAGE} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @Transactional
    public void unclaimTask(String taskId, AccessControlResponse accessControlResponse) {
        final boolean granularPermissionFeatureEnabled = isGranularPermissionFeatureEnabled(
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );

        PermissionRequirements permissionsRequired;
        if (granularPermissionFeatureEnabled) {
            permissionsRequired = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);
        } else {
            permissionsRequired = PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        }

        boolean taskHasUnassigned;

        TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        String taskState = taskResource.getState().getValue();
        taskHasUnassigned = taskState.equals(CFTTaskState.UNASSIGNED.getValue());

        String userId = accessControlResponse.getUserInfo().getUid();
        if (granularPermissionFeatureEnabled
            && taskResource.getAssignee() != null && !userId.equals(taskResource.getAssignee())
            && !checkUserHasUnassignPermission(accessControlResponse.getRoleAssignments(),
                                               taskResource.getTaskRoleResources())) {
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }

        unClaimTask(taskId, taskHasUnassigned);
    }

    private void unClaimTask(String taskId, boolean taskHasUnassigned) {
        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        task.setState(CFTTaskState.UNASSIGNED);
        task.setAssignee(null);
        //Perform Camunda updates
        camundaService.unclaimTask(taskId, taskHasUnassigned);
        //Commit transaction
        cftTaskDatabaseService.saveTask(task);
    }

    private boolean checkUserHasUnassignPermission(List<RoleAssignment> roleAssignments,
                                                   Set<TaskRoleResource> taskRoleResources) {
        for (RoleAssignment roleAssignment: roleAssignments) {
            String roleName = roleAssignment.getRoleName();
            for (TaskRoleResource taskRoleResource: taskRoleResources) {
                if (roleName.equals(taskRoleResource.getRoleName())
                    && Boolean.TRUE.equals(taskRoleResource.getUnassign())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Assigns the task to another user in Camunda.
     * Also performs role assignment verifications for both assignee and assigner.
     * This method requires:
     * Assigner to have {@link PermissionTypes#MANAGE} permission.
     * Assignee to have {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param taskId                        the task id.
     * @param assignerAccessControlResponse Assigner's access control response containing user id and role assignments.
     * @param assigneeAccessControlResponse Assignee's access control response containing user id and role assignments.
     */
    @Transactional
    public void assignTask(String taskId,
                           AccessControlResponse assignerAccessControlResponse,
                           Optional<AccessControlResponse> assigneeAccessControlResponse) {
        requireNonNull(assignerAccessControlResponse.getUserInfo().getUid(), "Assigner userId cannot be null");

        UserInfo assigner = assignerAccessControlResponse.getUserInfo();
        Optional<String> currentAssignee = cftTaskDatabaseService.findByIdOnly(taskId)
            .filter(t -> CFTTaskState.ASSIGNED.equals(t.getState()))
            .map(TaskResource::getAssignee);
        Optional<UserInfo> assignee = assigneeAccessControlResponse.map(AccessControlResponse::getUserInfo);

        if (verifyActionRequired(currentAssignee, assignee)) {
            final boolean granularPermissionEnabled = isGranularPermissionFeatureEnabled(
                assignerAccessControlResponse.getUserInfo().getUid(),
                assignerAccessControlResponse.getUserInfo().getEmail());

            PermissionRequirements assignerPermissionsRequired = assignerPermissionRequirement(
                granularPermissionEnabled,
                assigner,
                assignee,
                currentAssignee
            );
            //Verify assigner role assignments
            TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                taskId,
                assignerAccessControlResponse.getRoleAssignments(),
                assignerPermissionsRequired,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER
            );

            if (assignee.isEmpty()) {
                String taskState = taskResource.getState().getValue();
                boolean taskHasUnassigned = taskState.equals(CFTTaskState.UNASSIGNED.getValue());
                unClaimTask(taskId, taskHasUnassigned);
            } else {
                requireNonNull(assignee.get().getUid(), "Assignee userId cannot be null");

                PermissionRequirements assigneePermissionsRequired = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);
                List<RoleAssignment> roleAssignments = assigneeAccessControlResponse
                    .map(AccessControlResponse::getRoleAssignments).orElse(List.of());

                roleAssignmentVerification.verifyRoleAssignments(
                    taskId,
                    roleAssignments,
                    assigneePermissionsRequired,
                    ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
                );

                //Lock & update Task
                TaskResource task = findByIdAndObtainLock(taskId);
                task.setState(CFTTaskState.ASSIGNED);
                task.setAssignee(assignee.get().getUid());

                //Perform Camunda updates
                camundaService.assignTask(
                    taskId,
                    assignee.get().getUid(),
                    false
                );

                //Commit transaction
                cftTaskDatabaseService.saveTask(task);
            }
        }
    }

    private boolean verifyActionRequired(Optional<String> currentAssignee,
                                         Optional<UserInfo> assignee) {

        return (currentAssignee.isPresent()
            || assignee.isPresent())
            && (currentAssignee.isEmpty()
            || assignee.isEmpty()
            || !currentAssignee.get().equals(assignee.get().getUid()));
    }

    private PermissionRequirements assignerPermissionRequirement(boolean granularPermissionEnabled,
                                                                 UserInfo assigner,
                                                                 Optional<UserInfo> assignee,
                                                                 Optional<String> currentAssignee) {
        if (granularPermissionEnabled) {
            if (currentAssignee.isEmpty() && assignee.isPresent()) {
                return getPermissionToAssignAnUnassignedTask(assigner, assignee.get());
            } else if (assignee.isPresent()) {
                return getPermissionToAssignAnAssignedTask(assigner, assignee.get(), currentAssignee.get());
            } else {
                //Task is assigned to someone and assignee is no one
                return PermissionRequirementBuilder.builder().buildSingleRequirementWithOr(UNASSIGN, UNCLAIM);
            }
        } else {
            return PermissionRequirementBuilder.builder().buildSingleType(MANAGE);
        }
    }

    private PermissionRequirements getPermissionToAssignAnAssignedTask(UserInfo assigner,
                                                                       UserInfo assignee,
                                                                       String currentAssignee) {
        String assigneeUid = assignee.getUid();

        if (!assigner.getUid().equals(currentAssignee)
            && assigner.getUid().equals(assigneeUid)) {
            //Task is assigned  to someone else and requester tries to assign it to themselves
            return PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_CLAIM)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, CLAIM), AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), AND)
                .build();
        } else if (assigner.getUid().equals(currentAssignee)
            && !assigner.getUid().equals(assigneeUid)) {
            //Task is assigned to requester and requester tries to assign it to someone new
            return PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNCLAIM_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNCLAIM, ASSIGN), AND)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), AND)
                .build();
        } else {
            //When assigner tries to assign own task again themselves, it will be filtered out before come here.
            //Task is assigned to someone else and requester tries to assign it to someone new
            return PermissionRequirementBuilder.builder()
                .initPermissionRequirement(UNASSIGN_ASSIGN)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(List.of(UNASSIGN, ASSIGN), AND)
                .build();
        }
    }

    private PermissionRequirements getPermissionToAssignAnUnassignedTask(UserInfo assigner,
                                                                         UserInfo assignee) {
        //Task is unassigned and requester tries to assign task to someone
        PermissionRequirementBuilder builder = PermissionRequirementBuilder.builder().initPermissionRequirement(ASSIGN);

        if (assigner.getUid().equals(assignee.getUid())) {
            //Task is unassigned and requester tries to assign task to themselves
            return builder.joinPermissionRequirement(OR)
                .nextPermissionRequirement(CLAIM).build();
        }
        return builder.build();
    }

    /**
     * Cancels a task in camunda also performs role assignment verifications.
     * This method requires {@link PermissionTypes#CANCEL} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @Transactional
    public void cancelTask(String taskId,
                           AccessControlResponse accessControlResponse) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder().buildSingleType(CANCEL);

        roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );

        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        CFTTaskState previousTaskState = task.getState();
        task.setState(CFTTaskState.CANCELLED);

        boolean isCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

        log.info("{} previousTaskState : {} - isCftTaskStateExist : {}",
            taskId, previousTaskState, isCftTaskStateExist
        );

        try {
            //Perform Camunda updates
            camundaService.cancelTask(taskId);
            log.info("{} cancelled in camunda", taskId);
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);
            log.info("{} cancelled in CFT", taskId);
        } catch (TaskCancelException ex) {
            if (isCftTaskStateExist) {
                log.info("{} TaskCancelException occurred due to cftTaskState exists in Camunda.Exception: {}",
                    taskId, ex.getMessage()
                );
                throw ex;
            }

            if (!CFTTaskState.TERMINATED.equals(previousTaskState)) {
                task.setState(CFTTaskState.TERMINATED);
                cftTaskDatabaseService.saveTask(task);
                log.info("{} setting CFTTaskState to TERMINATED. previousTaskState : {} ",
                    taskId, previousTaskState
                );
                return;
            }

            log.info("{} Camunda Task appears to be Terminated but could not update the CFT Task state. "
                     + "CurrentCFTTaskState: {} Exception: {}", taskId, previousTaskState, ex.getMessage());
            throw ex;
        }
    }

    /**
     * Completes a task in camunda also performs role assignment verifications.
     * This method requires {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @Transactional
    public void completeTask(String taskId, AccessControlResponse accessControlResponse) {

        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        final String userId = accessControlResponse.getUserInfo().getUid();
        final String userEmail = accessControlResponse.getUserInfo().getEmail();

        final boolean isGranularPermissionFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            userId,
            userEmail
        );

        boolean taskHasCompleted = false;

        checkCompletePermissions(taskId, accessControlResponse, isGranularPermissionFeatureEnabled, userId);

        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        taskHasCompleted = task.getState() == CFTTaskState.COMPLETED;

        if (!taskHasCompleted) {
            //scenario, task not completed anywhere
            task.setState(CFTTaskState.COMPLETED);

            //check the state, if not complete, complete
            boolean isTaskCompleted = camundaService.isTaskCompletedInCamunda(taskId);
            if (!isTaskCompleted) {
                //Perform Camunda updates
                camundaService.completeTask(taskId, taskHasCompleted);
            }
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);
        }
    }

    private void checkCompletePermissions(String taskId, AccessControlResponse accessControlResponse,
                                          boolean isGranularPermissionFeatureEnabled, String userId) {
        PermissionRequirements permissionsRequired;
        if (isGranularPermissionFeatureEnabled) {
            permissionsRequired = PermissionRequirementBuilder.builder()
                .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(asList(COMPLETE), OR)
                .joinPermissionRequirement(OR)
                .nextPermissionRequirement(asList(COMPLETE_OWN), OR)
                .build();
        } else {
            permissionsRequired = PermissionRequirementBuilder.builder()
                .buildSingleRequirementWithOr(OWN, EXECUTE);
        }

        TaskResource taskResource;
        taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );

        //Safe-guard
        if (isGranularPermissionFeatureEnabled) {
            checkAssignee(taskResource, userId, taskId,
                          accessControlResponse.getRoleAssignments());
        } else {
            checkAssignee(taskResource.getAssignee(), userId, taskId);
        }
    }

    private void checkAssignee(TaskResource taskResource, String userId, String taskId,
                               List<RoleAssignment> roleAssignments) {
        if (!checkUserHasCompletePermission(roleAssignments, taskResource.getTaskRoleResources())) {
            if (taskResource.getAssignee() == null) {
                throw new TaskStateIncorrectException(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                );
            } else if (!userId.equals(taskResource.getAssignee())) {
                throw new TaskStateIncorrectException(
                    String.format("Could not complete task with id: %s as task was assigned to other user %s",
                                  taskId, taskResource.getAssignee()
                    )
                );
            }
        }
    }

    private void checkAssignee(String taskAssignee, String userId, String taskId) {
        if (taskAssignee == null) {
            throw new TaskStateIncorrectException(
                String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
            );
        } else if (!userId.equals(taskAssignee)) {
            throw new TaskStateIncorrectException(
                String.format("Could not complete task with id: %s as task was assigned to other user %s",
                              taskId, taskAssignee)
            );
        }
    }

    private boolean checkUserHasCompletePermission(List<RoleAssignment> roleAssignments,
                                                   Set<TaskRoleResource> taskRoleResources) {
        if (roleAssignments != null) {
            for (RoleAssignment roleAssignment : roleAssignments) {
                String roleName = roleAssignment.getRoleName();
                if (taskRoleResources != null) {
                    for (TaskRoleResource taskRoleResource : taskRoleResources) {
                        if (roleName.equals(taskRoleResource.getRoleName())
                            && Boolean.TRUE.equals(taskRoleResource.getComplete())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }



    /**
     * This method is only used by privileged clients allowing them to set a predefined options for completion.
     * This method requires {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param taskId                The task id to complete.
     * @param accessControlResponse the access control response containing user id and role assignments.
     * @param completionOptions     The completion options to orchestrate how this completion should be handled.
     */
    @Transactional
    public void completeTaskWithPrivilegeAndCompletionOptions(String taskId,
                                                              AccessControlResponse accessControlResponse,
                                                              CompletionOptions completionOptions) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        boolean taskStateIsAssignedAlready;
        if (completionOptions.isAssignAndComplete()) {

            TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                taskId,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            );

            final CFTTaskState state = taskResource.getState();
            taskStateIsAssignedAlready = state.getValue().equals(CFTTaskState.ASSIGNED.getValue());

            //Lock & update Task
            TaskResource task = findByIdAndObtainLock(taskId);
            task.setState(CFTTaskState.COMPLETED);
            //Perform Camunda updates
            camundaService.assignAndCompleteTask(
                taskId,
                accessControlResponse.getUserInfo().getUid(),
                taskStateIsAssignedAlready
            );
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);

        } else {
            completeTask(taskId, accessControlResponse);
        }
    }

    /**
     * Exclusive client access only.
     * This method terminates a task and orchestrates the logic between CFT Task db and camunda.
     * This method is transactional so if any exception occurs when calling camunda the transaction will be reverted.
     *
     * @param taskId        the task id.
     * @param terminateInfo Additional data to define how a task should be terminated.
     */
    @Transactional
    public void terminateTask(String taskId, TerminateInfo terminateInfo) {
        TaskResource task = null;
        try {
            //Find and Lock Task
            task = findByIdAndObtainLock(taskId);
        } catch (ResourceNotFoundException e) {
            //Perform Camunda updates
            log.warn("Task for id {} not found in the database, trying delete the task in camunda if exist", taskId);
            camundaService.deleteCftTaskState(taskId);
            return;
        }

        //Terminate the task if found in the database
        if (task != null) {
            //Update cft task and terminate reason
            task.setState(CFTTaskState.TERMINATED);
            task.setTerminationReason(terminateInfo.getTerminateReason());
            //Perform Camunda updates
            camundaService.deleteCftTaskState(taskId);
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);
        }
    }

    /**
     * Exclusive client access only.
     * This method initiates a task and orchestrates the logic between CFT Task db, camunda and role assignment.
     *
     * @param taskId              the task id.
     * @param initiateTaskRequest Additional data to define how a task should be initiated.
     * @return The updated entity {@link TaskResource}
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResource initiateTask(String taskId, InitiateTaskRequestAttributes initiateTaskRequest) {
        //Get DueDatetime or throw exception
        List<TaskAttribute> taskAttributes = initiateTaskRequest.getTaskAttributes();

        OffsetDateTime dueDate = extractDueDate(taskAttributes);

        lockTaskId(taskId, dueDate);
        return initiateTaskProcess(taskId, initiateTaskRequest);
    }

    /**
     * Exclusive client access only.
     * This method initiates a task and orchestrates the logic between CFT Task db, camunda and role assignment.
     *
     * @param taskId              the task id.
     * @param initiateTaskRequest Additional data to define how a task should be initiated.
     * @return The updated entity {@link TaskResource}
     */
    @Transactional(rollbackFor = Exception.class)
    public TaskResource initiateTask(String taskId, InitiateTaskRequestMap initiateTaskRequest) {
        //Get DueDatetime or throw exception
        Map<String, Object> taskAttributes = new ConcurrentHashMap<>(initiateTaskRequest.getTaskAttributes());
        taskAttributes.put("taskId", taskId);

        OffsetDateTime dueDate = extractDueDate(taskAttributes);

        lockTaskId(taskId, dueDate);
        return initiateTaskProcess(taskId, taskAttributes);
    }

    @Transactional
    public TaskResource updateNotes(String taskId, NotesRequest notesRequest) {
        validateNoteRequest(notesRequest);

        final TaskResource taskResource = findByIdAndObtainLock(taskId);

        final List<NoteResource> noteResources = notesRequest.getNoteResource();

        if (taskResource.getNotes() == null) {
            taskResource.setNotes(noteResources);
        } else {
            noteResources.forEach(noteResource -> taskResource.getNotes().add(noteResource));
        }
        taskResource.setHasWarnings(true);

        return cftTaskDatabaseService.saveTask(taskResource);
    }

    public Optional<TaskResource> getTaskById(String taskId) {
        return cftTaskDatabaseService.findByIdOnly(taskId);
    }

    /**
     * Retrieve task role information.
     * This method retrieves role permission information for a given task.
     * The task should have a read permission to retrieve role permission information.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     * @return collection of roles
     */
    public List<TaskRolePermissions> getTaskRolePermissions(String taskId,
                                                            AccessControlResponse accessControlResponse) {
        final Optional<TaskResource> taskResource = getTaskById(taskId);
        if (taskResource.isEmpty()) {
            throw new TaskNotFoundException(TASK_NOT_FOUND_ERROR);
        }

        if (taskResource.get().getTaskRoleResources().isEmpty()) {
            return emptyList();
        }

        SelectTaskResourceQueryBuilder selectQueryBuilder = new SelectTaskResourceQueryBuilder(entityManager);
        CriteriaBuilder builder = selectQueryBuilder.builder;
        Root<TaskResource> root = selectQueryBuilder.root;

        final Predicate selectPredicate = TaskSearchQueryBuilder.buildTaskRolePermissionsQuery(
            taskResource.get().getTaskId(), accessControlResponse.getRoleAssignments(), builder, root);

        final Optional<TaskResource> taskResourceQueryResult = selectQueryBuilder
            .where(selectPredicate)
            .build()
            .getResultList()
            .stream()
            .findFirst();

        if (taskResourceQueryResult.isEmpty()) {
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }

        return taskResourceQueryResult.get().getTaskRoleResources().stream()
            .map(cftTaskMapper::mapToTaskRolePermissions)
            .sorted(Comparator.comparing(TaskRolePermissions::getRoleName))
            .collect(Collectors.toList()
            );
    }

    public List<TaskResource> performOperation(TaskOperationRequest taskOperationRequest) {
        return taskOperationServices.stream()
            .flatMap(taskOperationService -> taskOperationService.performOperation(taskOperationRequest).stream())
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Helper method to extract the due date form the attributes this method.
     * Also includes validation that may throw a CustomConstraintViolationException.
     *
     * @param taskAttributes the task attributes
     * @return the due date
     */
    private OffsetDateTime extractDueDate(List<TaskAttribute> taskAttributes) {
        Map<TaskAttributeDefinition, Object> attributes = taskAttributes.stream()
            .filter(attribute -> attribute != null && attribute.getValue() != null)
            .collect(Collectors.toMap(TaskAttribute::getName, TaskAttribute::getValue));

        OffsetDateTime dueDate = cftTaskMapper.readDate(attributes, TASK_DUE_DATE, null);

        if (dueDate == null) {
            Violation violation = new Violation(
                TASK_DUE_DATE.value(),
                "Each task to initiate must contain task_due_date field present and populated."
            );
            throw new CustomConstraintViolationException(singletonList(violation));
        }
        return dueDate;
    }

    /**
     * Helper method to extract the due date form the attributes this method.
     * Also includes validation that may throw a CustomConstraintViolationException.
     *
     * @param taskAttributes the task attributes
     * @return the due date
     */
    private OffsetDateTime extractDueDate(Map<String, Object> taskAttributes) {
        Map<CamundaVariableDefinition, Object> attributes = taskAttributes.entrySet().stream()
            .filter(key -> CamundaVariableDefinition.from(key.getKey()).isPresent())
            .collect(Collectors.toMap(
                key -> CamundaVariableDefinition.from(key.getKey()).get(),
                Map.Entry::getValue
            ));
        OffsetDateTime dueDate = cftTaskMapper.readDate(attributes, DUE_DATE, null);

        if (dueDate == null) {
            Violation violation = new Violation(
                DUE_DATE.value(),
                "Each task to initiate must contain dueDate field present and populated."
            );
            throw new CustomConstraintViolationException(singletonList(violation));
        }
        return dueDate;
    }


    @SuppressWarnings("PMD.PreserveStackTrace")
    private TaskResource initiateTaskProcess(String taskId,
                                             InitiateTaskRequestAttributes initiateTaskRequest) {
        try {
            TaskResource taskResource = cftTaskMapper.mapToTaskResource(
                taskId,
                initiateTaskRequest.getTaskAttributes()
            );
            String roleAssignmentId = getRoleAssignmentId(initiateTaskRequest.getTaskAttributes());

            taskResource = configureTask(taskResource, roleAssignmentId);
            boolean isOldAssigneeValid = false;

            if (taskResource.getAssignee() != null) {
                log.info("Task '{}' had previous assignee, checking validity.", taskId);
                //Task had previous assignee
                isOldAssigneeValid =
                    taskAutoAssignmentService.checkAssigneeIsStillValid(taskResource, taskResource.getAssignee());
            }

            if (isOldAssigneeValid) {
                log.info("Task '{}' had previous assignee, and was valid, keeping assignee.", taskId);
                //Keep old assignee from skeleton task and change state
                taskResource.setState(CFTTaskState.ASSIGNED);
            } else {
                log.info("Task '{}' has an invalid assignee, unassign it before auto-assigning.", taskId);
                if (taskResource.getAssignee() != null) {
                    taskResource.setAssignee(null);
                    taskResource.setState(CFTTaskState.UNASSIGNED);
                }
                log.info("Task '{}' did not have previous assignee or was invalid, attempting to auto-assign.", taskId);
                //Otherwise attempt auto-assignment
                taskResource = taskAutoAssignmentService.autoAssignCFTTask(taskResource);
            }

            updateCftTaskState(taskResource.getTaskId(), taskResource);
            return cftTaskDatabaseService.saveTask(taskResource);
        } catch (Exception e) {
            log.error("Error when initiating task(id={})", taskId, e);
            throw new GenericServerErrorException(ErrorMessages.INITIATE_TASK_PROCESS_ERROR);
        }
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private TaskResource initiateTaskProcess(String taskId, Map<String, Object> taskAttributes) {
        try {
            TaskResource taskResource = cftTaskMapper.mapToTaskResource(
                taskId,
                taskAttributes
            );

            taskAttributes.put(DUE_DATE.value(), taskResource.getDueDateTime());

            taskResource = configureTask(taskResource, taskAttributes);
            boolean isOldAssigneeValid = false;

            if (taskResource.getAssignee() != null) {
                log.info("Task '{}' had previous assignee, checking validity.", taskId);
                //Task had previous assignee
                isOldAssigneeValid =
                    taskAutoAssignmentService.checkAssigneeIsStillValid(taskResource, taskResource.getAssignee());
            }

            if (isOldAssigneeValid) {
                log.info("Task '{}' had previous assignee, and was valid, keeping assignee.", taskId);
                //Keep old assignee from skeleton task and change state
                taskResource.setState(CFTTaskState.ASSIGNED);
            } else {
                log.info("Task '{}' has an invalid assignee, unassign it before auto-assigning.", taskId);
                if (taskResource.getAssignee() != null) {
                    taskResource.setAssignee(null);
                    taskResource.setState(CFTTaskState.UNASSIGNED);
                }
                log.info("Task '{}' did not have previous assignee or was invalid, attempting to auto-assign.", taskId);
                //Otherwise attempt auto-assignment
                taskResource = taskAutoAssignmentService.autoAssignCFTTask(taskResource);
            }

            updateCftTaskState(taskResource.getTaskId(), taskResource);
            return cftTaskDatabaseService.saveTask(taskResource);
        } catch (Exception e) {
            log.error("Error when initiating task(id={})", taskId, e);
            throw new GenericServerErrorException(ErrorMessages.INITIATE_TASK_PROCESS_ERROR);
        }
    }

    private String getRoleAssignmentId(List<TaskAttribute> taskAttributes) {

        return taskAttributes.stream()
            .filter(attribute -> {
                log.debug("filtering out null attributes: attribute({})", attribute);
                return attribute != null && attribute.getValue() != null;
            })
            .filter(attribute -> attribute.getName().value().equals(TASK_ROLE_ASSIGNMENT_ID.value()))
            .map(TaskAttribute::getValue)
            .findFirst()
            .map(Object::toString)
            .orElse(null);
    }

    @SuppressWarnings("PMD.PreserveStackTrace")
    private void lockTaskId(String taskId, OffsetDateTime dueDate) {
        try {
            cftTaskDatabaseService.insertAndLock(taskId, dueDate);
        } catch (DataAccessException | SQLException e) {
            log.error("Error when inserting and locking the task(id={})", taskId, e);
            throw new DatabaseConflictException(ErrorMessages.DATABASE_CONFLICT_ERROR);
        }
    }

    private void updateCftTaskState(String taskId, TaskResource taskResource) {
        if (CFTTaskState.ASSIGNED.equals(taskResource.getState())) {
            camundaService.updateCftTaskState(taskId, TaskState.ASSIGNED);
        } else if (CFTTaskState.UNASSIGNED.equals(taskResource.getState())) {
            camundaService.updateCftTaskState(taskId, TaskState.UNASSIGNED);
        }
    }

    private TaskResource configureTask(TaskResource taskSkeleton, String roleAssignmentId) {
        Map<String, Object> taskAttributes = new ConcurrentHashMap<>(cftTaskMapper.getTaskAttributes(taskSkeleton));
        Optional.ofNullable(roleAssignmentId).ifPresent((id) -> taskAttributes.put("roleAssignmentId", id));
        TaskToConfigure taskToConfigure = new TaskToConfigure(
            taskSkeleton.getTaskId(),
            taskSkeleton.getTaskType(),
            taskSkeleton.getCaseId(),
            taskSkeleton.getTaskName(),
            taskAttributes
        );

        return configureTaskService.configureCFTTask(
            taskSkeleton,
            taskToConfigure
        );
    }

    private TaskResource configureTask(TaskResource taskSkeleton, Map<String, Object> taskAttributes) {
        TaskToConfigure taskToConfigure = new TaskToConfigure(
            taskSkeleton.getTaskId(),
            taskSkeleton.getTaskType(),
            taskSkeleton.getCaseId(),
            taskSkeleton.getTaskName(),
            taskAttributes
        );

        return configureTaskService.configureCFTTask(
            taskSkeleton,
            taskToConfigure
        );
    }

    private TaskResource findByIdAndObtainLock(String taskId) {
        return cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    @SuppressWarnings({"PMD.PrematureDeclaration"})
    private void validateNoteRequest(NotesRequest notesRequest) {
        String errorMessage = "must not be empty";
        if (notesRequest == null || notesRequest.getNoteResource() == null) {
            throw new InvalidRequestException("Invalid request message");
        }

        if (notesRequest.getNoteResource().isEmpty()) {
            Violation violation = new Violation(
                "note_resource",
                errorMessage
            );
            throw new CustomConstraintViolationException(singletonList(violation));
        }

        notesRequest.getNoteResource().forEach(nt -> {
            if (nt == null) {
                Violation violation = new Violation(
                    "note_resource",
                    errorMessage
                );
                throw new CustomConstraintViolationException(singletonList(violation));
            }

            if (nt.getCode() == null || nt.getCode().isEmpty()) {
                Violation violation = new Violation(
                    "code",
                    errorMessage
                );
                throw new CustomConstraintViolationException(singletonList(violation));
            }

            if (nt.getNoteType() == null || nt.getNoteType().isEmpty()) {
                Violation violation = new Violation(
                    "note_type",
                    errorMessage
                );
                throw new CustomConstraintViolationException(singletonList(violation));
            }
        });
    }

    private boolean isGranularPermissionFeatureEnabled(String userId, String email) {
        return launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.GRANULAR_PERMISSION_FEATURE,
                userId,
                email
            );
    }


}
