package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.GRANULAR_PERMISSION_FEATURE;

@ExtendWith(MockitoExtension.class)
class CompleteTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    @Mock
    CamundaService camundaService;
    @Mock
    CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    CftQueryService cftQueryService;
    @Mock
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
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
    private List<TaskOperationService> taskOperationServices;


    @Test
    void completeTask_should_succeed() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(0)).isTaskCompletedInCamunda(taskId);

    }

    @Test
    void completeTask_should_succeed_and_gp_feature_flag_is_on() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(OWN, EXECUTE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(COMPLETE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(COMPLETE_OWN), OR)
            .build();

        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(true);

        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(0)).isTaskCompletedInCamunda(taskId);
    }

    @Test
    void completeTask_should_succeed_and_gp_feature_flag_is_off() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);

        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(false);

        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(0)).isTaskCompletedInCamunda(taskId);
    }

    @Test
    void completeTask_should_succeed_and_task_completed_in_camunda() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(0)).isTaskCompletedInCamunda(taskId);

    }

    @Test
    void completeTask_should_succeed_when_camunda_task_completion_fails_as_task_is_already_complete() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        doThrow(new TaskCompleteException(ErrorMessages.TASK_COMPLETE_UNABLE_TO_COMPLETE))
            .when(camundaService).completeTask(anyString(), anyBoolean());

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASEID"));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
        when(camundaService.isTaskCompletedInCamunda(taskId)).thenReturn(true);
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(1)).isTaskCompletedInCamunda(taskId);
    }

    @Test
    void completeTask_should_succeed_when_camunda_task_completion_fails_as_task_status_cannot_be_updated() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        doThrow(new TaskCompleteException(ErrorMessages.TASK_COMPLETE_UNABLE_TO_UPDATE_STATE))
            .when(camundaService).completeTask(anyString(), anyBoolean());

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASEID"));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(camundaService.isTaskCompletedInCamunda(taskId)).thenReturn(false);
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        assertThatThrownBy(() -> taskManagementService.completeTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(TaskCompleteException.class)
            .hasNoCause()
            .hasMessage("Task Complete Error: Task complete failed. Unable to update task state to completed.");

        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, false);
        verify(camundaService, times(1)).isTaskCompletedInCamunda(taskId);
    }

    @Test
    void completeTask_should_succeed_and_task_not_completed_in_camunda() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
        verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
        verify(camundaService, times(0)).isTaskCompletedInCamunda(taskId);
    }

    @Test
    void completeTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        RoleAssignment roleAssignment = mock(RoleAssignment.class);
        when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
        List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.empty());
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));

        assertThatThrownBy(() -> taskManagementService.completeTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
    }

    @Test
    void completeTask_should_throw_task_state_incorrect_exception_when_task_has_no_assignee_gp_flag_is_off() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .buildSingleRequirementWithOr(OWN, EXECUTE);
        when(cftQueryService.getTask(taskId, accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(taskResource.getAssignee()).thenReturn(null);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.completeTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(TaskStateIncorrectException.class)
            .hasNoCause()
            .hasMessage(
                String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
            );

        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
    }

    @Test
    void completeTask_should_throw_task_state_incorrect_exception_when_task_has_no_assignee_gp_flag_is_on() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);

        PermissionRequirements requirements = PermissionRequirementBuilder.builder()
            .initPermissionRequirement(asList(OWN, EXECUTE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(COMPLETE), OR)
            .joinPermissionRequirement(OR)
            .nextPermissionRequirement(asList(COMPLETE_OWN), OR)
            .build();

        when(cftTaskDatabaseService.findCaseId(taskId)).thenReturn(Optional.of("CASE_ID"));
        when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), requirements))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(null);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(true);

        assertThatThrownBy(() -> taskManagementService.completeTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(TaskStateIncorrectException.class)
            .hasNoCause()
            .hasMessage(
                String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
            );

        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
    }

    @Test
    void completeTask_should_throw_exception_when_missing_required_arguments() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

        assertThatThrownBy(() -> taskManagementService.completeTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("UserId cannot be null");
    }

    @Test
    void should_throw_exception_when_task_resource_not_found() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);

        assertThatThrownBy(() -> taskManagementService.completeTask(taskId, accessControlResponse))
            .isInstanceOf(TaskNotFoundException.class)
            .hasNoCause()
            .hasMessage("Task Not Found Error: The task could not be found.");
        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
    }

    @Test
    void should_throw_exception_when_task_resource_not_found_and_gp_feature_flag_is_on() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(true);

        assertThatThrownBy(() -> taskManagementService.completeTask(taskId, accessControlResponse))
            .isInstanceOf(TaskNotFoundException.class)
            .hasNoCause()
            .hasMessage("Task Not Found Error: The task could not be found.");
        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
    }

    @Test
    void should_throw_exception_when_task_resource_not_found_and_gp_feature_flag_is_off() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 GRANULAR_PERMISSION_FEATURE,
                 IDAM_USER_ID,
                 IDAM_USER_EMAIL
             )
        ).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.completeTask(taskId, accessControlResponse))
            .isInstanceOf(TaskNotFoundException.class)
            .hasNoCause()
            .hasMessage("Task Not Found Error: The task could not be found.");
        verify(camundaService, times(0)).completeTask(any(), anyBoolean());
        verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
    }

    @BeforeEach
    public void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService
        );
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            taskOperationServices,
            entityManager,
            idamTokenGenerator
        );


        taskId = UUID.randomUUID().toString();
    }

}

