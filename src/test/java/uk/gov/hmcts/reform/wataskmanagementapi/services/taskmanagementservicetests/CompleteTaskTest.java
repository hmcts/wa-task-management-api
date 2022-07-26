package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE;

@ExtendWith(MockitoExtension.class)
class CompleteTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    @Mock
    CamundaService camundaService;
    @Mock
    CamundaQueryBuilder camundaQueryBuilder;
    @Mock
    PermissionEvaluatorService permissionEvaluatorService;
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

    RoleAssignmentVerificationService roleAssignmentVerification;

    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Mock
    private List<TaskOperationService> taskOperationServices;


    @Test
    void completeTask_should_succeed_and_feature_flag_is_on() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
            .thenReturn(Optional.of(taskResource));
        when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), asList(OWN, EXECUTE)))
            .thenReturn(Optional.of(taskResource));
        when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
        when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_ENDPOINTS_FEATURE,
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
    }

    @Test
    void completeTask_should_succeed_and_feature_flag_is_off() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
        when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            IDAM_USER_ID,
            IDAM_USER_ID,
            mockedVariables,
            roleAssignment,
            asList(OWN, EXECUTE)
        )).thenReturn(true);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
            )
        ).thenReturn(false);

        taskManagementService.completeTask(taskId, accessControlResponse);
        boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value().equals(mockedVariables.get("taskState"));
        verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
    }

    @Test
    void completeTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
        when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            IDAM_USER_ID,
            IDAM_USER_ID,
            mockedVariables,
            roleAssignment,
            asList(OWN, EXECUTE)
        )).thenReturn(false);

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
    void completeTask_should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
        CamundaTask mockedUnmappedTask = createMockedUnmappedTaskWithNoAssignee();
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_ENDPOINTS_FEATURE,
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
    void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

        final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
        when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

        TaskResource taskResource = spy(TaskResource.class);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_ENDPOINTS_FEATURE,
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

    @BeforeEach
    public void setUp() {
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftQueryService
        );
        taskManagementService = new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            taskOperationServices,
            entityManager,
            allowedJurisdictionConfiguration
        );


        taskId = UUID.randomUUID().toString();
    }

}

