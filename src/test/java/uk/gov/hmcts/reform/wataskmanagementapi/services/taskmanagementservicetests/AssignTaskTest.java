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
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;

@ExtendWith(MockitoExtension.class)
class AssignTaskTest extends CamundaHelpers {

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
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    TaskManagementService taskManagementService;
    String taskId;

    @Test
    void assignTask_should_succeed() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignmentAssigner = singletonList(mock(RoleAssignment.class));
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignmentAssignee = singletonList(mock(RoleAssignment.class));
        when(assigneeAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssignee);
        when(assigneeAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

        when(permissionEvaluatorService.hasAccess(
            mockedVariables,
            roleAssignmentAssigner,
            singletonList(MANAGE)
        )).thenReturn(true);
        when(permissionEvaluatorService.hasAccess(
            mockedVariables,
            roleAssignmentAssignee,
            asList(OWN, EXECUTE)
        )).thenReturn(true);

        taskManagementService.assignTask(taskId, assignerAccessControlResponse, assigneeAccessControlResponse);

        verify(camundaService, times(1)).assignTask(taskId, IDAM_USER_ID, mockedVariables);
    }

    @Test
    void assignTask_should_throw_role_assignment_verification_exception_when_assigner_has_access_returns_false() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignmentAssigner = singletonList(mock(RoleAssignment.class));
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        when(assigneeAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

        when(permissionEvaluatorService.hasAccess(
            mockedVariables,
            roleAssignmentAssigner,
            singletonList(MANAGE)
        )).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).assignTask(any(), any(), any());
    }

    @Test
    void assignTask_should_throw_role_assignment_verification_exception_when_assignee_has_access_returns_false() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignmentAssigner = singletonList(mock(RoleAssignment.class));
        when(assignerAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssigner);
        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignmentAssignee = singletonList(mock(RoleAssignment.class));
        when(assigneeAccessControlResponse.getRoleAssignments()).thenReturn(roleAssignmentAssignee);
        when(assigneeAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

        when(permissionEvaluatorService.hasAccess(
            mockedVariables,
            roleAssignmentAssigner,
            singletonList(MANAGE)
        )).thenReturn(true);

        when(permissionEvaluatorService.hasAccess(
            mockedVariables,
            roleAssignmentAssignee,
            asList(OWN, EXECUTE)
        )).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).assignTask(any(), any(), any());
    }

    @Test
    void assignTask_should_throw_exception_when_missing_required_arguments() {
        AccessControlResponse assignerAccessControlResponse = mock(AccessControlResponse.class);
        when(assignerAccessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());

        AccessControlResponse assigneeAccessControlResponse = mock(AccessControlResponse.class);

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("Assigner userId cannot be null");

        when(assignerAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(SECONDARY_IDAM_USER_ID).build());
        when(assigneeAccessControlResponse.getUserInfo())
            .thenReturn(UserInfo.builder().uid(null).build());

        assertThatThrownBy(() -> taskManagementService.assignTask(
            taskId,
            assignerAccessControlResponse,
            assigneeAccessControlResponse
        ))
            .isInstanceOf(NullPointerException.class)
            .hasNoCause()
            .hasMessage("Assignee userId cannot be null");

    }

    @BeforeEach
    public void setUp() {
        taskManagementService = new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService
        );

        taskId = UUID.randomUUID().toString();
    }
}

