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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTWorkTypeDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;

@ExtendWith(MockitoExtension.class)
class UnclaimTaskTest extends CamundaHelpers {

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
    @Mock
    CFTWorkTypeDatabaseService cftWorkTypeDatabaseService;
    TaskManagementService taskManagementService;
    String taskId;

    @Test
    void unclaimTask_should_succeed() {
        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
        when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
        CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
        when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            IDAM_USER_ID,
            IDAM_USER_ID,
            mockedVariables,
            roleAssignment,
            singletonList(MANAGE)
        )).thenReturn(true);

        taskManagementService.unclaimTask(taskId, accessControlResponse);

        verify(camundaService, times(1)).unclaimTask(taskId, mockedVariables);
    }

    @Test
    void unclaimTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

        AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
        List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
        when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
        when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
        CamundaTask mockedUnmappedTask = createMockedUnmappedTask();
        Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);
        when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
        when(permissionEvaluatorService.hasAccessWithAssigneeCheckAndHierarchy(
            IDAM_USER_ID,
            IDAM_USER_ID,
            mockedVariables,
            roleAssignment,
            singletonList(MANAGE)
        )).thenReturn(false);

        assertThatThrownBy(() -> taskManagementService.unclaimTask(
            taskId,
            accessControlResponse
        ))
            .isInstanceOf(RoleAssignmentVerificationException.class)
            .hasNoCause()
            .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        verify(camundaService, times(0)).unclaimTask(any(), any());
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

