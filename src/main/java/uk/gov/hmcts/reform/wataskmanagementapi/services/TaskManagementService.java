package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.violations.Violation;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.SelectTaskResourceQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskSearchQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.NotesRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
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
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.TASK_NOT_FOUND_ERROR;

@Slf4j
@Service
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.DataflowAnomalyAnalysis",
    "PMD.ExcessiveImports",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveParameterList"})
public class TaskManagementService {
    public static final String USER_ID_CANNOT_BE_NULL = "UserId cannot be null";

    private final CamundaService camundaService;
    private final CamundaQueryBuilder camundaQueryBuilder;
    private final CFTTaskDatabaseService cftTaskDatabaseService;
    private final CFTTaskMapper cftTaskMapper;
    private final LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    private final ConfigureTaskService configureTaskService;
    private final TaskAutoAssignmentService taskAutoAssignmentService;
    private final List<TaskOperationService> taskOperationServices;
    private final RoleAssignmentVerificationService roleAssignmentVerification;

    @PersistenceContext
    private final EntityManager entityManager;

    private final AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Autowired
    public TaskManagementService(CamundaService camundaService,
                                 CamundaQueryBuilder camundaQueryBuilder,
                                 CFTTaskDatabaseService cftTaskDatabaseService,
                                 CFTTaskMapper cftTaskMapper,
                                 LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider,
                                 ConfigureTaskService configureTaskService,
                                 TaskAutoAssignmentService taskAutoAssignmentService,
                                 RoleAssignmentVerificationService roleAssignmentVerification,
                                 List<TaskOperationService> taskOperationServices,
                                 EntityManager entityManager,
                                 AllowedJurisdictionConfiguration allowedJurisdictionConfiguration) {
        this.camundaService = camundaService;
        this.camundaQueryBuilder = camundaQueryBuilder;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.cftTaskMapper = cftTaskMapper;
        this.launchDarklyFeatureFlagProvider = launchDarklyFeatureFlagProvider;
        this.configureTaskService = configureTaskService;
        this.taskAutoAssignmentService = taskAutoAssignmentService;
        this.taskOperationServices = taskOperationServices;
        this.roleAssignmentVerification = roleAssignmentVerification;
        this.entityManager = entityManager;
        this.allowedJurisdictionConfiguration = allowedJurisdictionConfiguration;
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
        List<PermissionTypes> permissionsRequired = singletonList(READ);

        final boolean isFeatureEnabled = launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );
        if (isFeatureEnabled) {
            TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
            Set<PermissionTypes> permissionsUnionForUser =
                cftTaskMapper.extractUnionOfPermissionsForUser(
                    taskResource.getTaskRoleResources(),
                    accessControlResponse.getRoleAssignments()
                );

            return cftTaskMapper.mapToTaskWithPermissions(taskResource, permissionsUnionForUser);
        } else {
            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
            roleAssignmentVerification.verifyRoleAssignments(
                variables, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
            return camundaService.getMappedTask(taskId, variables);
        }
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
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);

        final boolean isFeatureEnabled = launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                userId, accessControlResponse.getUserInfo().getEmail()
            );
        if (isFeatureEnabled) {
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

        } else {
            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
            roleAssignmentVerification.verifyRoleAssignments(
                variables, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
            camundaService.claimTask(taskId, accessControlResponse.getUserInfo().getUid());
        }

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
        String userId = accessControlResponse.getUserInfo().getUid();
        List<PermissionTypes> permissionsRequired = singletonList(MANAGE);
        boolean taskHasUnassigned;
        final boolean isFeatureEnabled = launchDarklyFeatureFlagProvider
            .getBooleanValue(
                FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );
        if (isFeatureEnabled) {
            TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
            String taskState = taskResource.getState().getValue();
            taskHasUnassigned = taskState.equals(CFTTaskState.UNASSIGNED.getValue());

            //Lock & update Task
            TaskResource task = findByIdAndObtainLock(taskId);
            task.setState(CFTTaskState.UNASSIGNED);
            task.setAssignee(null);
            //Perform Camunda updates
            camundaService.unclaimTask(taskId, taskHasUnassigned);
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);
        } else {
            CamundaTask camundaTask = camundaService.getUnmappedCamundaTask(taskId);
            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);

            roleAssignmentVerification.roleAssignmentVerificationWithAssigneeCheckAndHierarchy(
                camundaTask.getAssignee(),
                userId,
                variables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            );
            String taskState = camundaService.getVariableValue(variables.get(TASK_STATE.value()), String.class);
            taskHasUnassigned = TaskState.UNASSIGNED.value().equals(taskState);
            camundaService.unclaimTask(taskId, taskHasUnassigned);
        }
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
                           AccessControlResponse assigneeAccessControlResponse) {
        requireNonNull(assignerAccessControlResponse.getUserInfo().getUid(), "Assigner userId cannot be null");
        requireNonNull(assigneeAccessControlResponse.getUserInfo().getUid(), "Assignee userId cannot be null");
        List<PermissionTypes> assignerPermissionsRequired = singletonList(MANAGE);
        List<PermissionTypes> assigneePermissionsRequired = List.of(OWN, EXECUTE);

        Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
        String assigneeUserId = assigneeAccessControlResponse.getUserInfo().getUid();

        final boolean isRelease2EndpointsFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            assignerAccessControlResponse.getUserInfo().getUid(),
            assignerAccessControlResponse.getUserInfo().getEmail()
        );
        if (isRelease2EndpointsFeatureEnabled) {
            roleAssignmentVerification.verifyRoleAssignments(
                taskId,
                assignerAccessControlResponse.getRoleAssignments(),
                assignerPermissionsRequired,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER
            );

            roleAssignmentVerification.verifyRoleAssignments(
                taskId,
                assigneeAccessControlResponse.getRoleAssignments(),
                assigneePermissionsRequired,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
            );

            //Lock & update Task
            TaskResource task = findByIdAndObtainLock(taskId);
            task.setState(CFTTaskState.ASSIGNED);
            task.setAssignee(assigneeUserId);

            //Perform Camunda updates
            camundaService.assignTask(
                taskId,
                assigneeUserId,
                false
            );
            //Commit transaction
            cftTaskDatabaseService.saveTask(task);

        } else {
            roleAssignmentVerification.verifyRoleAssignments(
                variables,
                assignerAccessControlResponse.getRoleAssignments(),
                assignerPermissionsRequired,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNER
            );
            roleAssignmentVerification.verifyRoleAssignments(
                variables,
                assigneeAccessControlResponse.getRoleAssignments(),
                assigneePermissionsRequired,
                ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
            );

            String taskState = camundaService.getVariableValue(variables.get(TASK_STATE.value()), String.class);
            boolean isTaskStateAssigned = TaskState.ASSIGNED.value().equals(taskState);
            camundaService.assignTask(
                taskId,
                assigneeUserId,
                isTaskStateAssigned
            );
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
                           AccessControlResponse accessControlResponse) {
        requireNonNull(accessControlResponse.getUserInfo().getUid(), USER_ID_CANNOT_BE_NULL);
        List<PermissionTypes> permissionsRequired = singletonList(CANCEL);

        final boolean isRelease2EndpointsFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            accessControlResponse.getUserInfo().getUid(),
            accessControlResponse.getUserInfo().getEmail()
        );

        if (isRelease2EndpointsFeatureEnabled) {
            roleAssignmentVerification.verifyRoleAssignments(
                taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
        } else {
            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
            roleAssignmentVerification.verifyRoleAssignments(
                variables, accessControlResponse.getRoleAssignments(), permissionsRequired
            );
        }

        if (isRelease2EndpointsFeatureEnabled) {
            //Lock & update Task
            TaskResource task = findByIdAndObtainLock(taskId);
            CFTTaskState previousTaskState = task.getState();
            task.setState(CFTTaskState.CANCELLED);

            boolean isCftTaskStateExist = camundaService.isCftTaskStateExistInCamunda(taskId);

            log.info("{} previousTaskState : {} - isCftTaskStateExist : {}",
                taskId, previousTaskState, isCftTaskStateExist);

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
                        taskId, ex.getMessage());
                    throw ex;
                }

                if (!CFTTaskState.TERMINATED.equals(previousTaskState)) {
                    task.setState(CFTTaskState.TERMINATED);
                    cftTaskDatabaseService.saveTask(task);
                    log.info("{} setting CFTTaskState to TERMINATED. previousTaskState : {} ",
                        taskId, previousTaskState);
                    return;
                }

                log.info("{} Camunda Task appears to be Terminated but could not update the CFT Task state. "
                         + "CurrentCFTTaskState: {} Exception: {}", taskId, previousTaskState, ex.getMessage());
                throw ex;
            }

        } else {
            camundaService.cancelTask(taskId);
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
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
        boolean taskHasCompleted = false;
        final String userId = accessControlResponse.getUserInfo().getUid();
        final String userEmail = accessControlResponse.getUserInfo().getEmail();

        final boolean isRelease2EndpointsFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            userId,
            userEmail
        );

        if (isRelease2EndpointsFeatureEnabled) {
            TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                taskId, accessControlResponse.getRoleAssignments(), permissionsRequired
            );

            //Safe-guard
            if (taskResource.getAssignee() == null) {
                throw new TaskStateIncorrectException(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                );
            }
        } else {
            CamundaTask camundaTask = camundaService.getUnmappedCamundaTask(taskId);

            //Safe-guard
            if (camundaTask.getAssignee() == null) {
                throw new TaskStateIncorrectException(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                );
            }

            Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
            // Check that task state was not already completed
            String taskState = camundaService.getVariableValue(variables.get(TASK_STATE.value()), String.class);
            taskHasCompleted = TaskState.COMPLETED.value().equals(taskState);

            roleAssignmentVerification.roleAssignmentVerificationWithAssigneeCheckAndHierarchy(
                camundaTask.getAssignee(),
                userId,
                variables,
                accessControlResponse.getRoleAssignments(),
                permissionsRequired
            );
        }

        if (isRelease2EndpointsFeatureEnabled) {

            //Lock & update Task
            TaskResource task = findByIdAndObtainLock(taskId);
            taskHasCompleted = task.getState() == CFTTaskState.COMPLETED;

            // If task was not already completed complete it
            if (taskHasCompleted) {
                //Perform Camunda updates
                camundaService.completeTask(taskId, taskHasCompleted);
            } else {
                task.setState(CFTTaskState.COMPLETED);
                //Perform Camunda updates
                camundaService.completeTask(taskId, taskHasCompleted);
                //Commit transaction
                cftTaskDatabaseService.saveTask(task);
            }
        } else {
            camundaService.completeTask(taskId, taskHasCompleted);
        }
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
        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
        boolean taskStateIsAssignedAlready;
        if (completionOptions.isAssignAndComplete()) {
            final boolean isRelease2EndpointsFeatureEnabled = launchDarklyFeatureFlagProvider.getBooleanValue(
                FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                accessControlResponse.getUserInfo().getUid(),
                accessControlResponse.getUserInfo().getEmail()
            );

            if (isRelease2EndpointsFeatureEnabled) {
                TaskResource taskResource = roleAssignmentVerification.verifyRoleAssignments(
                    taskId,
                    accessControlResponse.getRoleAssignments(),
                    permissionsRequired
                );

                final CFTTaskState state = taskResource.getState();
                taskStateIsAssignedAlready = state.getValue().equals(CFTTaskState.ASSIGNED.getValue());

            } else {
                Map<String, CamundaVariable> variables = camundaService.getTaskVariables(taskId);
                roleAssignmentVerification.verifyRoleAssignments(
                    variables, accessControlResponse.getRoleAssignments(), permissionsRequired
                );

                String taskState = camundaService.getVariableValue(variables.get(TASK_STATE.value()), String.class);
                taskStateIsAssignedAlready = TaskState.ASSIGNED.value().equals(taskState);

            }


            if (isRelease2EndpointsFeatureEnabled) {
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
                camundaService.assignAndCompleteTask(
                    taskId,
                    accessControlResponse.getUserInfo().getUid(),
                    taskStateIsAssignedAlready
                );
            }

        } else {
            completeTask(taskId, accessControlResponse);
        }
    }

    /**
     * Performs a search in camunda and retrieves mapped tasks also filters out tasks by role assignments permissions.
     * This method requires {@link PermissionTypes#READ} permission.
     * This method supports pagination parameters.
     *
     * @param searchTaskRequest     the search request.
     * @param firstResult           pagination parameter the first result where to begin searching.
     * @param maxResults            pagination parameter the max results to returns.
     * @param accessControlResponse the access control response containing user id and role assignments.
     * @return a list of filtered and mapped tasks {@link Task}
     */
    public List<Task> searchWithCriteria(SearchTaskRequest searchTaskRequest,
                                         int firstResult, int maxResults,
                                         AccessControlResponse accessControlResponse) {

        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);

        //Safe-guard to avoid sending empty orQueries to camunda and abort early
        if (query == null) {
            return emptyList();
        }
        List<PermissionTypes> permissionsRequired = singletonList(READ);
        return camundaService.searchWithCriteria(
            query,
            firstResult,
            maxResults,
            accessControlResponse,
            permissionsRequired
        );
    }

    /**
     * Performs a specific search in camunda to find tasks that could be completed.
     * This method requires {@link PermissionTypes#OWN} or {@link PermissionTypes#EXECUTE} permission.
     *
     * @param searchEventAndCase    the search request.
     * @param accessControlResponse the access control response containing user id and role assignments.
     */
    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    public GetTasksCompletableResponse<Task> searchForCompletableTasks(SearchEventAndCase searchEventAndCase,
                                                                       AccessControlResponse accessControlResponse) {
        //Safe-guard against unsupported Jurisdictions
        if (!allowedJurisdictionConfiguration.getAllowedJurisdictions()
            .contains(searchEventAndCase.getCaseJurisdiction().toLowerCase(Locale.ROOT))
            || !allowedJurisdictionConfiguration.getAllowedCaseTypes()
            .contains(searchEventAndCase.getCaseType().toLowerCase(Locale.ROOT))
        ) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        //1. Evaluate Dmn
        final List<Map<String, CamundaVariable>> evaluateDmnResult = camundaService.evaluateTaskCompletionDmn(
            searchEventAndCase);

        // Collect task types
        List<String> taskTypes = extractTaskTypes(evaluateDmnResult);

        if (taskTypes.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        //2. Build query and perform search
        CamundaSearchQuery camundaSearchQuery =
            camundaQueryBuilder.createCompletableTasksQuery(searchEventAndCase.getCaseId(), taskTypes);
        //3. Perform the search
        List<CamundaTask> searchResults = camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery);

        //Safe guard in case no search results were returned
        if (searchResults.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }
        //4. Extract if a task is assigned and assignee is idam userId
        String idamUserId = accessControlResponse.getUserInfo().getUid();

        final List<CamundaTask> tasksAssignedToUser = searchResults.stream()
            .filter(task -> idamUserId.equals(task.getAssignee()))
            .collect(Collectors.toList());

        if (!tasksAssignedToUser.isEmpty()) {
            searchResults = tasksAssignedToUser;
        }

        List<PermissionTypes> permissionsRequired = asList(OWN, EXECUTE);
        final List<Task> taskList = camundaService.performSearchAction(
            searchResults,
            accessControlResponse,
            permissionsRequired
        );

        if (taskList.isEmpty()) {
            return new GetTasksCompletableResponse<>(false, emptyList());
        }

        boolean taskRequiredForEvent = isTaskRequired(evaluateDmnResult, taskTypes);

        return new GetTasksCompletableResponse<>(taskRequiredForEvent, taskList);
    }

    /**
     * Retrieve the total amount of tasks based on a query.
     *
     * @param searchTaskRequest the search request.
     * @return the amount of tasks for a specific query.
     */
    public long getTaskCount(SearchTaskRequest searchTaskRequest) {
        CamundaSearchQuery query = camundaQueryBuilder.createQuery(searchTaskRequest);
        //Safe-guard to avoid sending empty orQueries to camunda and abort early
        if (query == null) {
            return 0;
        }
        return camundaService.getTaskCount(query);
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
    public TaskResource initiateTask(String taskId, InitiateTaskRequest initiateTaskRequest) {
        //Get DueDatetime or throw exception
        List<TaskAttribute> taskAttributes = initiateTaskRequest.getTaskAttributes();

        OffsetDateTime dueDate = extractDueDate(taskAttributes);

        lockTaskId(taskId, dueDate);
        return initiateTaskProcess(taskId, initiateTaskRequest);
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

    @SuppressWarnings("PMD.PreserveStackTrace")
    private TaskResource initiateTaskProcess(String taskId, InitiateTaskRequest initiateTaskRequest) {
        try {
            TaskResource taskResource = createTaskSkeleton(taskId, initiateTaskRequest);
            taskResource = configureTask(taskResource);
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

    private TaskResource configureTask(TaskResource taskSkeleton) {
        TaskToConfigure taskToConfigure = new TaskToConfigure(
            taskSkeleton.getTaskId(),
            taskSkeleton.getTaskType(),
            taskSkeleton.getCaseId(),
            taskSkeleton.getTaskName()
        );

        return configureTaskService.configureCFTTask(
            taskSkeleton,
            taskToConfigure
        );
    }

    private TaskResource createTaskSkeleton(String taskId, InitiateTaskRequest initiateTaskRequest) {
        return cftTaskMapper.mapToTaskResource(
            taskId,
            initiateTaskRequest.getTaskAttributes()
        );
    }

    private TaskResource findByIdAndObtainLock(String taskId) {
        return cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));
    }

    private boolean isTaskRequired(List<Map<String, CamundaVariable>> evaluateDmnResult, List<String> taskTypes) {
        /*
         * EvaluateDmnResult contains with and without empty rows for an event.
         * TaskTypes are extracted from evaluateDmnResult.
         * If both the sizes are equal, it means there is no empty row and task is required for the event
         * If they are of different sizes, it means there is an empty row and task is not required
         */
        return evaluateDmnResult.size() == taskTypes.size();
    }

    private List<String> extractTaskTypes(List<Map<String, CamundaVariable>> evaluateDmnResult) {
        return evaluateDmnResult.stream()
            .filter(result -> result.containsKey(TASK_TYPE.value()))
            .map(result -> camundaService.getVariableValue(result.get(TASK_TYPE.value()), String.class))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

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
