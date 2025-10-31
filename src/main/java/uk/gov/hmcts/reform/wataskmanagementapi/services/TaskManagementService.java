package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.SelectTaskResourceQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskSearchQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.task.TaskRolePermissions;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.GenericServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.InvalidRequestException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.ServiceMandatoryFieldValidationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.AND;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.OR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNASSIGN_CLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.UNCLAIM_ASSIGN;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController.REQ_PARAM_CANCELLATION_PROCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController.REQ_PARAM_COMPLETION_PROCESS;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.ADD_WARNING;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.AUTO_CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.TERMINATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.TERMINATE_EXCEPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.MANDATORY_FIELD_MISSING_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_NOT_FOUND_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.TaskActionAttributesBuilder.buildTaskActionAttributeForAssign;


@Slf4j
@Service
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.DataflowAnomalyAnalysis",
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveParameterList",
    "PMD.ExcessiveClassLength",
    "PMD.GodClass",
    "PMD.UnusedAssignment",
    "PMD.CyclomaticComplexity",
    "PMD.CognitiveComplexity"})
public class TaskManagementService {
    public static final String USER_ID_CANNOT_BE_NULL = "UserId cannot be null";
    public static final String REQUEST_PARAM_MAP_CANNOT_BE_NULL = "Request param map cannot be null";
    private final CamundaService camundaService;
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CFTTaskMapper cftTaskMapper;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;
    private final RoleAssignmentVerificationService roleAssignmentVerification;
    private final IdamTokenGenerator idamTokenGenerator;
    private final CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    private final TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @PersistenceContext
    private final EntityManager entityManager;

    @Autowired
    public TaskManagementService(CamundaService camundaService,
                                 CFTTaskDatabaseService cftTaskDatabaseService,
                                 CFTTaskMapper cftTaskMapper,
                                 ConfigureTaskService configureTaskService,
                                 TaskAutoAssignmentService taskAutoAssignmentService,
                                 RoleAssignmentVerificationService roleAssignmentVerification,
                                 EntityManager entityManager,
                                 IdamTokenGenerator idamTokenGenerator,
                                 CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService,
                                 TaskMandatoryFieldsValidator taskMandatoryFieldsValidator) {
        this.camundaService = camundaService;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.cftTaskMapper = cftTaskMapper;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
        this.roleAssignmentVerification = roleAssignmentVerification;
        this.entityManager = entityManager;
        this.idamTokenGenerator = idamTokenGenerator;
        this.cftSensitiveTaskEventLogsDatabaseService = cftSensitiveTaskEventLogsDatabaseService;
        this.taskMandatoryFieldsValidator = taskMandatoryFieldsValidator;
    }

    protected void updateTaskActionAttributesForAssign(TaskResource taskResource, String assigner,
                                                       Optional<String> newAssignee,
                                                       Optional<String> oldAssignee) {
        TaskAction taskAction = buildTaskActionAttributeForAssign(assigner, newAssignee, oldAssignee);
        if (taskAction != null) {
            setTaskActionAttributes(taskResource, assigner, taskAction);
        }
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

        log.info("task resource due date before conversion {}", taskResource.getDueDateTime());

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

        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(CLAIM, OWN), AND)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(CLAIM, EXECUTE), AND)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(ASSIGN, EXECUTE), AND)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(ASSIGN, OWN), AND)
            .build();


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
        setTaskActionAttributes(task, userId, TaskAction.CLAIM);

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
        log.info("GP for {} and {} is {}", accessControlResponse.getUserInfo().getUid(),
                 accessControlResponse.getUserInfo().getEmail()
        );
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(UNCLAIM, UNASSIGN);

        boolean taskHasUnassigned;

        TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );
        String taskState = taskResource.getState().getValue();
        taskHasUnassigned = taskState.equals(CFTTaskState.UNASSIGNED.getValue());

        String userId = accessControlResponse.getUserInfo().getUid();
        if (taskResource.getAssignee() != null && !userId.equals(taskResource.getAssignee())
            && !checkUserHasUnassignPermission(
            accessControlResponse.getRoleAssignments(),
            taskResource.getTaskRoleResources()
        )) {
            cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
                taskId,
                accessControlResponse.getRoleAssignments(),
                ROLE_ASSIGNMENT_VERIFICATIONS_FAILED
            );
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }

        unclaimTask(taskId, userId, taskHasUnassigned, TaskAction.UNCLAIM);
    }

    private void unclaimTask(String taskId, String userId, boolean taskHasUnassigned, TaskAction taskAction) {
        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        task.setState(CFTTaskState.UNASSIGNED);
        task.setAssignee(null);
        setTaskActionAttributes(task, userId, taskAction);
        //Perform Camunda updates
        camundaService.unclaimTask(taskId, taskHasUnassigned);
        //Commit transaction
        cftTaskDatabaseService.saveTask(task);
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
            PermissionRequirements assignerPermissionsRequired = assignerPermissionRequirement(
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
                TaskAction taskAction = buildTaskActionAttributeForAssign(assigner.getUid(), Optional.empty(),
                                                                          currentAssignee);
                unclaimTask(taskId, assigner.getUid(), taskHasUnassigned, taskAction);
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
                updateTaskActionAttributesForAssign(task, assigner.getUid(),
                                                    Optional.of(assignee.get().getUid()), currentAssignee);
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

    /**
     * Cancels a task in camunda also performs role assignment verifications.
     * This method requires {@link PermissionTypes#CANCEL} permission.
     *
     * @param taskId                the task id.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @Transactional
    public void cancelTask(String taskId,
                           AccessControlResponse accessControlResponse, Map<String, Object> requestParamMap) {
        TerminationProcess terminationProcess = null;
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        requireNonNull(requestParamMap, REQUEST_PARAM_MAP_CANNOT_BE_NULL);
        if (requestParamMap.get(REQ_PARAM_CANCELLATION_PROCESS) != null) {
            final String cancellationProcess = requestParamMap.get(REQ_PARAM_CANCELLATION_PROCESS).toString();
            terminationProcess = TerminationProcess.fromValue(cancellationProcess);
        }
        PermissionRequirements permissionsRequired;

        String userId = accessControlResponse.getUserInfo().getUid();

        permissionsRequired = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(CANCEL, CANCEL_OWN);

        TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );

        if (!taskResource.getTaskRoleResources().stream().anyMatch(permission -> permission.getCancel().equals(true))
            && (taskResource.getAssignee() == null
            || !userId.equals(taskResource.getAssignee()))) {
            cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
                taskId,
                accessControlResponse.getRoleAssignments(),
                ROLE_ASSIGNMENT_VERIFICATIONS_FAILED
            );
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }

        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        CFTTaskState previousTaskState = task.getState();
        task.setState(CFTTaskState.CANCELLED);
        task.setTerminationProcess(terminationProcess);

        boolean isCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

        log.info("{} previousTaskState : {} - isCftTaskStateExist : {}",
                 taskId, previousTaskState, isCftTaskStateExist
        );

        try {
            //Perform Camunda updates
            camundaService.cancelTask(taskId);
            log.info("{} cancelled in camunda", taskId);

            //set task action attributes
            setTaskActionAttributes(task, userId, TaskAction.CANCEL);

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
     * @param requestParamMap       the termination process using which task is completed
     */
    @Transactional
    public void completeTask(String taskId, AccessControlResponse accessControlResponse,
                             Map<String, Object> requestParamMap) {
        TerminationProcess terminationProcess = null;
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        requireNonNull(requestParamMap, REQUEST_PARAM_MAP_CANNOT_BE_NULL);
        final String userId = accessControlResponse.getUserInfo().getUid();
        if (requestParamMap.get(REQ_PARAM_COMPLETION_PROCESS) != null) {
            final String completionProcess = requestParamMap.get(REQ_PARAM_COMPLETION_PROCESS).toString();
            terminationProcess = TerminationProcess.fromValue(completionProcess);
        }
        boolean taskHasCompleted;

        checkCompletePermissions(taskId, accessControlResponse, userId);

        //Lock & update Task
        TaskResource task = findByIdAndObtainLock(taskId);
        CFTTaskState state = task.getState();
        taskHasCompleted = state != null
            && (state.equals(CFTTaskState.COMPLETED)
            || state.equals(CFTTaskState.TERMINATED)
            && task.getTerminationReason().equals("completed"));

        if (!taskHasCompleted) {
            //scenario, task not completed anywhere
            //check the state, if not complete, complete
            completeCamundaTask(taskId, taskHasCompleted);
            //Commit transaction
            if (task.isActive(state)) {
                task.setState(CFTTaskState.COMPLETED);
                task.setTerminationProcess(terminationProcess);
                setTaskActionAttributes(task, userId, TaskAction.COMPLETED);
                cftTaskDatabaseService.saveTask(task);
            }
        }
    }

    /**
     * This method is only used by privileged clients allowing them to set a predefined options for completion.
     * This method requires {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param taskId                The task id to complete.
     * @param accessControlResponse the access control response containing user id and role assignments.
     * @param completionOptions     The completion options to orchestrate how this completion should be handled.
     * @param requestParamMap       the termination process using which task is completed
     */
    @Transactional
    public void completeTaskWithPrivilegeAndCompletionOptions(String taskId,
                                                              AccessControlResponse accessControlResponse,
                                                              CompletionOptions completionOptions,
                                                              Map<String, Object> requestParamMap) {
        String userId = accessControlResponse.getUserInfo().getUid();
        TerminationProcess terminationProcess = null;
        requireNonNull(userId, USER_ID_CANNOT_BE_NULL);
        requireNonNull(requestParamMap, REQUEST_PARAM_MAP_CANNOT_BE_NULL);
        if (requestParamMap.get(REQ_PARAM_COMPLETION_PROCESS) != null) {
            final String completionProcess = requestParamMap.get(REQ_PARAM_COMPLETION_PROCESS).toString();
            terminationProcess = TerminationProcess.fromValue(completionProcess);
        }
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
            setTaskActionAttributes(task, userId, TaskAction.COMPLETED);
            //Perform Camunda updates
            camundaService.assignAndCompleteTask(
                taskId,
                userId,
                taskStateIsAssignedAlready
            );
            task.setTerminationProcess(terminationProcess);
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);

        } else {
            completeTask(taskId, accessControlResponse, requestParamMap);
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
            boolean isCamundaStateUpdated = false;
            try {
                TaskAction taskAction = switch (terminateInfo.getTerminateReason()) {
                    case "cancelled" -> AUTO_CANCEL;
                    case "completed" -> TERMINATE;
                    default -> TERMINATE_EXCEPTION;
                };
                setSystemUserTaskActionAttributes(task, taskAction);
                //Perform Camunda updates
                camundaService.deleteCftTaskState(taskId);
                isCamundaStateUpdated = true;
                // Commit transaction
                cftTaskDatabaseService.saveTask(task);
            } catch (Exception ex) {
                if (isCamundaStateUpdated) {
                    log.error("Error saving task with id {} after successfully deleting Camunda task state: {}",
                              taskId, ex.getMessage(), ex);
                }
                log.error("Error occurred while terminating task with id: {}", taskId, ex);
                throw ex;
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateTaskIndex(String taskId) {
        try {
            Optional<TaskResource> findTaskResponse = cftTaskDatabaseService
                .findByIdAndWaitAndObtainPessimisticWriteLock(taskId);

            if (findTaskResponse.isPresent()) {
                TaskResource taskResource = findTaskResponse.get();
                taskResource.setIndexed(true);

                cftTaskDatabaseService.saveTask(taskResource);
            } else {
                throw new TaskNotFoundException(TASK_NOT_FOUND_ERROR);
            }
        } catch (PersistenceException ex) {
            log.error("PersistenceException occurred in updating indexed field of taskId:{}", taskId);
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
        setSystemUserTaskActionAttributes(taskResource, ADD_WARNING);
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
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
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
            cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
                taskId,
                accessControlResponse.getRoleAssignments(),
                ROLE_ASSIGNMENT_VERIFICATIONS_FAILED
            );
            throw new RoleAssignmentVerificationException(ROLE_ASSIGNMENT_VERIFICATIONS_FAILED);
        }

        return taskResourceQueryResult.get().getTaskRoleResources().stream()
            .map(r -> cftTaskMapper.mapToTaskRolePermissions(r))
            .sorted(Comparator.comparing(TaskRolePermissions::getRoleName))
            .toList();
    }

    private void setTaskActionAttributes(TaskResource task, String userId, TaskAction action) {
        task.setLastUpdatedTimestamp(OffsetDateTime.now());
        task.setLastUpdatedUser(userId);
        task.setLastUpdatedAction(action.getValue());
    }

    private boolean checkUserHasUnassignPermission(List<RoleAssignment> roleAssignments,
                                                   Set<TaskRoleResource> taskRoleResources) {
        for (RoleAssignment roleAssignment : roleAssignments) {
            String roleName = roleAssignment.getRoleName();
            for (TaskRoleResource taskRoleResource : taskRoleResources) {
                if (roleName.equals(taskRoleResource.getRoleName())
                    && Boolean.TRUE.equals(taskRoleResource.getUnassign())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean verifyActionRequired(Optional<String> currentAssignee,
                                         Optional<UserInfo> assignee) {

        return (currentAssignee.isPresent()
            || assignee.isPresent())
            && (currentAssignee.isEmpty()
            || assignee.isEmpty()
            || !currentAssignee.get().equals(assignee.get().getUid()));
    }

    private PermissionRequirements assignerPermissionRequirement(UserInfo assigner,
                                                                 Optional<UserInfo> assignee,
                                                                 Optional<String> currentAssignee) {
        if (currentAssignee.isEmpty() && assignee.isPresent()) {
            return getPermissionToAssignAnUnassignedTask(assigner, assignee.get());
        } else if (assignee.isPresent()) {
            return getPermissionToAssignAnAssignedTask(assigner, assignee.get(), currentAssignee.get());
        } else {
            //Task is assigned to someone and assignee is no one
            return PermissionRequirementBuilder.builder().buildSingleRequirementWithOr(UNASSIGN, UNCLAIM);
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

    private void completeCamundaTask(String taskId, boolean taskHasCompleted) {
        try {
            //Perform Camunda updates
            camundaService.completeTask(taskId, taskHasCompleted);
        } catch (TaskCompleteException e) {
            boolean isTaskCompleted = camundaService.isTaskCompletedInCamunda(taskId);
            if (!isTaskCompleted) {
                log.error("Task Completion failed for task ({}) due to {}.", taskId, e.getMessage());
                throw e;
            }

            log.error("Task Completion failed for task ({}) as task is already complete", taskId);
        }
    }

    private void checkCompletePermissions(String taskId, AccessControlResponse accessControlResponse,
                                          String userId) {
        PermissionRequirements permissionsRequired = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(OWN, EXECUTE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(List.of(COMPLETE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
            .build();

        TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
            taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
        );

        //Safe-guard
        checkAssignee(taskResource, userId, taskId,
                      accessControlResponse.getRoleAssignments());

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

    private boolean checkUserHasCompletePermission(
        List<RoleAssignment> roleAssignments,
        Set<TaskRoleResource> taskRoleResources
    ) {
        if (roleAssignments == null || taskRoleResources == null) {
            return false;
        }

        for (RoleAssignment roleAssignment : roleAssignments) {
            String roleName = roleAssignment.getRoleName();
            if (hasCompletePermissionForRole(taskRoleResources, roleName)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasCompletePermissionForRole(Set<TaskRoleResource> taskRoleResources, String roleName) {
        return taskRoleResources.stream()
            .anyMatch(taskRoleResource -> roleName.equals(taskRoleResource.getRoleName())
                && Boolean.TRUE.equals(taskRoleResource.getComplete()));
    }

    private void setSystemUserTaskActionAttributes(TaskResource taskResource, TaskAction taskAction) {
        String systemUserToken = idamTokenGenerator.generate();
        String systemUserId = idamTokenGenerator.getUserInfo(systemUserToken).getUid();
        setTaskActionAttributes(taskResource, systemUserId, taskAction);
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
    private TaskResource initiateTaskProcess(String taskId, Map<String, Object> taskAttributes) {
        try {
            TaskResource taskResource = cftTaskMapper.mapToTaskResource(
                taskId,
                taskAttributes
            );

            taskAttributes.put(DUE_DATE.value(), taskResource.getDueDateTime());

            taskResource = configureTask(taskResource, taskAttributes);
            taskMandatoryFieldsValidator.validate(taskResource);
            taskResource = taskAutoAssignmentService.performAutoAssignment(taskId, taskResource);
            updateCftTaskState(taskResource.getTaskId(), taskResource);
            return cftTaskDatabaseService.saveTask(taskResource);
        } catch (FeignException e) {
            log.error("Error when initiating task(id={})", taskId, e);
            throw e;
        } catch (ServiceMandatoryFieldValidationException e) {
            log.error("Error when initiating task(id={})", taskId, e);
            throw new ServiceMandatoryFieldValidationException(MANDATORY_FIELD_MISSING_ERROR.getDetail()
                                                                   + taskId + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error when initiating task(id={})", taskId, e);
            throw new GenericServerErrorException(ErrorMessages.INITIATE_TASK_PROCESS_ERROR);
        }
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
}
