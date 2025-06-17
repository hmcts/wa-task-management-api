package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirementBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionJoin.OR;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.COMPLETE_OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.TaskActionsController.REQ_PARAM_COMPLETION_PROCESS;

@ExtendWith(MockitoExtension.class)
class CompleteTaskWithPrivilegeAndCompletionOptionsTest extends CamundaHelpers {

    @Mock
    CamundaService camundaService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    IdamTokenGenerator idamTokenGenerator;
    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;
    @Mock
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    @Mock
    private List<TaskOperationPerformService> taskOperationPerformServices;


    @BeforeEach
    void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            entityManager,
            idamTokenGenerator,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator);


        taskId = UUID.randomUUID().toString();
    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class CompleteTaskWithPrivilegeAndCompletionOptions {

        @Test
        void should_throw_exception_when_missing_required_arguments() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {

            @Test
            void should_succeed() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);


                TaskResource taskResource = spy(TaskResource.class);

                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .buildSingleRequirementWithOr(OWN, EXECUTE);
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);
                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
                HashMap<String, Object> requestParamMap = new HashMap<>();
                requestParamMap.put(REQ_PARAM_COMPLETION_PROCESS, "EXUI_USER_COMPLETION");

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true),
                    requestParamMap
                );

                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                assertEquals(TerminationProcess.EXUI_USER_COMPLETION, taskResource.getTerminationProcess());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                boolean taskStateIsAssignededAlready = TaskState.ASSIGNED.value()
                    .equals(mockedVariables.get("taskState"));
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, taskStateIsAssignededAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_is_false_and_completion_options() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        new HashMap<>()
                    ));
                assertEquals(
                    "Role Assignment Verification: "
                    + "The request failed the Role Assignment checks performed.",
                    exception.getMessage()
                );

            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {
            @Test
            void should_succeed_and_gp_feature_flag_is_on() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
                when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
                when(taskResource.getState()).thenReturn(CFTTaskState.ASSIGNED);
                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                    .build();
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false),
                    new HashMap<>()
                );
                boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value()
                    .equals(mockedVariables.get("taskState"));
                verify(taskResource, times(1)).getState();
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

                Exception exception = assertThrowsExactly(RoleAssignmentVerificationException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        new HashMap<>()
                    ));
                assertEquals(
                    "Role Assignment Verification: "
                    + "The request failed the Role Assignment checks performed.",
                    exception.getMessage()
                );

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }

            @Test
            void should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                PermissionRequirements requirements = PermissionRequirementBuilder.builder()
                    .initPermissionRequirement(asList(OWN, EXECUTE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE), OR)
                    .joinPermissionRequirement(OR)
                    .nextPermissionRequirement(List.of(COMPLETE_OWN), OR)
                    .build();
                when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
                when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
                    .thenReturn(Optional.of(taskResource));

                Exception exception = assertThrowsExactly(TaskStateIncorrectException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        new HashMap<>()
                    ));
                assertEquals(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId),
                    exception.getMessage()
                );

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }


            @Test
            void should_throw_exception_when_task_resource_not_found() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                Exception exception = assertThrowsExactly(TaskNotFoundException.class, () ->
                    taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        new HashMap<>()
                    ));
                assertEquals(
                    "Task Not Found Error: The task could not be found.",
                    exception.getMessage()
                );

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }
        }
    }

}

