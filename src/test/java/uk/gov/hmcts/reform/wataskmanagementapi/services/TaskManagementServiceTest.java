package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.GetTasksCompletableResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaSearchQuery;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Task;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.TaskStateIncorrectException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.RoleAssignmentVerificationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.EXECUTE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.MANAGE;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.OWN;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.entities.PermissionTypes.READ;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

@ExtendWith(MockitoExtension.class)
class TaskManagementServiceTest extends CamundaHelpers {


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
    TaskManagementService taskManagementService;
    String taskId;

    @BeforeEach
    public void setUp() {
        taskManagementService = new TaskManagementService(
            camundaService,
            camundaQueryBuilder,
            permissionEvaluatorService,
            cftTaskDatabaseService,
            cftTaskMapper,
            launchDarklyFeatureFlagProvider
        );

        taskId = UUID.randomUUID().toString();
    }

    @Nested
    @DisplayName("getTask()")
    class GetTask {
        @Test
        void getTask_should_succeed_and_return_mapped_task() {

            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            Task mockedMappedTask = mock(Task.class);
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(READ)
            )).thenReturn(true);
            when(camundaService.getMappedTask(taskId, mockedVariables)).thenReturn(mockedMappedTask);

            Task response = taskManagementService.getTask(taskId, roleAssignment);

            assertNotNull(response);
            assertEquals(mockedMappedTask, response);
        }

        @Test
        void getTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(READ)
            )).thenReturn(false);

            assertThatThrownBy(() -> taskManagementService.getTask(taskId, roleAssignment))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

        }
    }

    @Nested
    @DisplayName("claimTask()")
    class ClaimTask {
        @Test
        void claimTask_should_succeed() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            taskManagementService.claimTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).claimTask(taskId, IDAM_USER_ID);
        }

        @Test
        void claimTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                asList(OWN, EXECUTE)
            )).thenReturn(false);

            assertThatThrownBy(() -> taskManagementService.claimTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).claimTask(any(), any());
        }

        @Test
        void claimTask_should_throw_exception_when_missing_required_arguments() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(null).build());
            assertThatThrownBy(() -> taskManagementService.claimTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(NullPointerException.class)
                .hasNoCause()
                .hasMessage("UserId cannot be null");
        }

    }

    @Nested
    @DisplayName("unclaimTask()")
    class UnclaimTask {
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

    }


    @Nested
    @DisplayName("assignTask()")
    class AssignTask {
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
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {
        @Test
        void cancelTask_should_succeed_and_feature_flag_is_on() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(CANCEL)
            )).thenReturn(true);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(true);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
            verify(camundaService, times(1)).cancelTask(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }

        @Test
        void cancelTask_should_succeed_and_feature_flag_is_off() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(CANCEL)
            )).thenReturn(true);

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(false);

            taskManagementService.cancelTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).cancelTask(taskId);
        }

        @Test
        void cancelTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(CANCEL)
            )).thenReturn(false);

            assertThatThrownBy(() -> taskManagementService.cancelTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).cancelTask(any());
        }

        @Test
        void cancelTask_should_throw_exception_when_missing_required_arguments() {

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

        @Test
        void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
            when(permissionEvaluatorService.hasAccess(
                mockedVariables,
                roleAssignment,
                singletonList(CANCEL)
            )).thenReturn(true);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(true);

            assertThatThrownBy(() -> taskManagementService.cancelTask(taskId, accessControlResponse))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasNoCause()
                .hasMessage("Resource not found");
            verify(camundaService, times(0)).cancelTask(any());
            verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
        }
    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        @Test
        void completeTask_should_succeed_and_feature_flag_is_on() {
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
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            TaskResource taskResource = spy(TaskResource.class);
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(true);

            taskManagementService.completeTask(taskId, accessControlResponse);

            assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            verify(camundaService, times(1)).completeTask(taskId, mockedVariables);
        }

        @Test
        void completeTask_should_succeed_and_feature_flag_is_off() {
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
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(false);

            taskManagementService.completeTask(taskId, accessControlResponse);

            verify(camundaService, times(1)).completeTask(taskId, mockedVariables);
        }

        @Test
        void completeTask_should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

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
                asList(OWN, EXECUTE)
            )).thenReturn(false);

            assertThatThrownBy(() -> taskManagementService.completeTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(RoleAssignmentVerificationException.class)
                .hasNoCause()
                .hasMessage("Role Assignment Verification: The request failed the Role Assignment checks performed.");

            verify(camundaService, times(0)).completeTask(any(), any());
        }

        @Test
        void completeTask_should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            CamundaTask mockedUnmappedTask = createMockedUnmappedTaskWithNoAssignee();
            when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);

            assertThatThrownBy(() -> taskManagementService.completeTask(
                taskId,
                accessControlResponse
            ))
                .isInstanceOf(TaskStateIncorrectException.class)
                .hasNoCause()
                .hasMessage(
                    String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                );

            verify(camundaService, times(0)).completeTask(any(), any());
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
                asList(OWN, EXECUTE)
            )).thenReturn(true);

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
                 )
            ).thenReturn(true);

            assertThatThrownBy(() -> taskManagementService.completeTask(taskId, accessControlResponse))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasNoCause()
                .hasMessage("Resource not found");
            verify(camundaService, times(0)).completeTask(any(), any());
            verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
        }

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
            void should_succeed_and_feature_flag_is_on() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(true);

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                );

                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, mockedVariables);
            }

            @Test
            void should_succeed_and_feature_flag_is_off() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(false);

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                );

                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, mockedVariables);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_is_false_and_completion_options() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(false);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                ))
                    .isInstanceOf(RoleAssignmentVerificationException.class)
                    .hasNoCause()
                    .hasMessage("Role Assignment Verification: "
                                + "The request failed the Role Assignment checks performed.");
            }

            @Test
            void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
                    mockedVariables,
                    roleAssignment,
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(true);


                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Resource not found");
                verify(camundaService, times(0)).assignAndCompleteTask(any(), any(), any());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_succeed_and_feature_flag_is_on() {
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
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(true);

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                );

                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1)).completeTask(taskId, mockedVariables);
            }

            @Test
            void should_succeed_and_feature_flag_is_off() {
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
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(false);

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                );

                verify(camundaService, times(1)).completeTask(taskId, mockedVariables);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

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
                    asList(OWN, EXECUTE)
                )).thenReturn(false);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                ))
                    .isInstanceOf(RoleAssignmentVerificationException.class)
                    .hasNoCause()
                    .hasMessage("Role Assignment Verification: "
                                + "The request failed the Role Assignment checks performed.");

                verify(camundaService, times(0)).completeTask(any(), any());
            }

            @Test
            void should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
                CamundaTask mockedUnmappedTask = createMockedUnmappedTaskWithNoAssignee();
                when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(mockedUnmappedTask);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                ))
                    .isInstanceOf(TaskStateIncorrectException.class)
                    .hasNoCause()
                    .hasMessage(
                        String.format("Could not complete task with id: %s as task was not previously assigned", taskId)
                    );

                verify(camundaService, times(0)).completeTask(any(), any());
            }


            @Test
            void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {
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
                    asList(OWN, EXECUTE)
                )).thenReturn(true);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                    IDAM_USER_ID
                     )
                ).thenReturn(true);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                ))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Resource not found");

                verify(camundaService, times(0)).completeTask(any(), any());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }
        }
    }

    @Nested
    @DisplayName("searchWithCriteria()")
    class SearchWithCriteria {
        @Test
        void searchWithCriteria_should_succeed_and_return_emptyList() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(null);

            List<Task> response = taskManagementService.searchWithCriteria(
                searchTaskRequest,
                0,
                1,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(emptyList(), response);
        }

        @Test
        void searchWithCriteria_should_succeed_and_return_mapped_tasks() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            Task mockedMappedTask = createMockedMappedTask();
            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQuery);
            when(camundaService.searchWithCriteria(
                camundaSearchQuery,
                0,
                1,
                accessControlResponse,
                singletonList(READ)
            )).thenReturn(singletonList(mockedMappedTask));

            List<Task> response = taskManagementService.searchWithCriteria(
                searchTaskRequest,
                0,
                1,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(mockedMappedTask, response.get(0));
        }
    }

    @Nested
    @DisplayName("getTaskCount()")
    class GetTaskCount {
        @Test
        void getTaskCount_should_succeed_and_return_count() {
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(camundaSearchQuery);
            when(camundaService.getTaskCount(camundaSearchQuery)).thenReturn(Long.valueOf(50));

            long response = taskManagementService.getTaskCount(searchTaskRequest);

            assertEquals(50, response);
            verify(camundaService, times(1)).getTaskCount(camundaSearchQuery);
        }

        @Test
        void getTaskCount_should_succeed_and_return_count_when_query_is_null() {
            SearchTaskRequest searchTaskRequest = mock(SearchTaskRequest.class);
            when(camundaQueryBuilder.createQuery(searchTaskRequest)).thenReturn(null);

            long response = taskManagementService.getTaskCount(searchTaskRequest);

            assertEquals(0, response);
            verify(camundaService, times(0)).getTaskCount(any());
        }
    }


    @Nested
    @DisplayName("searchForCompletableTasks()")
    class SearchForCompletableTasks {

        @Test
        void should_succeed_and_return_emptyList_when_jurisdiction_is_not_IA() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "invalidJurisdiction",
                "Asylum"
            );

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_caseType_is_not_Asylum() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "someInvalidCaseType"
            );

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_no_task_types_returned() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase)).thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_no_search_results() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
                .thenReturn(camundaSearchQuery);

            when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_emptyList_when_performSearachAction_no_results() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
                .thenReturn(camundaSearchQuery);

            List<CamundaTask> searchResults = singletonList(createMockedUnmappedTask());
            when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .thenReturn(searchResults);

            when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
                .thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }


        @Test
        void should_succeed_and_return_emptyList_when_performSearchAction_no_results_no_assignee() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");
            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
                .thenReturn(camundaSearchQuery);

            List<CamundaTask> searchResults = singletonList(createMockedUnmappedTaskWithNoAssignee());
            when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .thenReturn(searchResults);

            when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
                .thenReturn(emptyList());

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, emptyList()), response);
        }

        @Test
        void should_succeed_and_return_tasks_and_is_required_true() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponse());
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");

            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
                .thenReturn(camundaSearchQuery);

            List<CamundaTask> searchResults = singletonList(createMockedUnmappedTaskWithNoAssignee());
            when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .thenReturn(searchResults);

            List<Task> mappedTasksResults = singletonList(createMockedMappedTask());
            when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
                .thenReturn(mappedTasksResults);

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(true, mappedTasksResults), response);
        }

        @Test
        void should_succeed_and_return_tasks_is_required_false() {
            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "someCaseId",
                "someEventId",
                "IA",
                "Asylum"
            );

            when(camundaService.evaluateTaskCompletionDmn(searchEventAndCase))
                .thenReturn(mockTaskCompletionDMNResponseWithEmptyRow());
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());
            when(camundaService.getVariableValue(any(), any())).thenReturn("reviewTheAppeal");
            CamundaSearchQuery camundaSearchQuery = mock(CamundaSearchQuery.class);
            when(camundaQueryBuilder.createCompletableTasksQuery(any(), any()))
                .thenReturn(camundaSearchQuery);

            List<CamundaTask> searchResults = singletonList(createMockedUnmappedTask());
            when(camundaService.searchWithCriteriaAndNoPagination(camundaSearchQuery))
                .thenReturn(searchResults);

            List<Task> mappedTasksResults = singletonList(createMockedMappedTask());
            when(camundaService.performSearchAction(searchResults, accessControlResponse, asList(OWN, EXECUTE)))
                .thenReturn(mappedTasksResults);

            GetTasksCompletableResponse<Task> response = taskManagementService.searchForCompletableTasks(
                searchEventAndCase,
                accessControlResponse
            );

            assertNotNull(response);
            assertEquals(new GetTasksCompletableResponse<>(false, mappedTasksResults), response);
        }
    }


    @Nested
    @DisplayName("initiateTask()")
    class InitiateTask {
        @Test
        void should_succeed() {

            InitiateTaskRequest req = new InitiateTaskRequest(
                INITIATION,
                asList(
                    new TaskAttribute(TASK_TYPE, "aTaskType"),
                    new TaskAttribute(TASK_NAME, "aTaskName")
                )
            );

            TaskResource taskResource = mock(TaskResource.class);
            when(cftTaskMapper.mapToTaskObject(taskId, req.getTaskAttributes())).thenReturn(taskResource);
            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.initiateTask(taskId, req);

            verify(cftTaskMapper, times(1)).mapToTaskObject(taskId, req.getTaskAttributes());
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }

    }


    @Nested
    @DisplayName("terminateTask()")
    class TerminateTask {

        @Nested
        @DisplayName("When Terminate Reason is Completed")
        class Completed {

            TerminateInfo terminateInfo = new TerminateInfo(TerminateReason.COMPLETED);

            @Test
            void should_succeed() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.terminateTask(taskId, terminateInfo);

                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            }

            @Test
            void should_throw_exception_when_task_resource_not_found() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                assertThatThrownBy(() -> taskManagementService.terminateTask(taskId, terminateInfo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Resource not found");
                verify(camundaService, times(0)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }


        @Nested
        @DisplayName("When Terminate Reason is Cancelled")
        class Cancelled {
            TerminateInfo terminateInfo = new TerminateInfo(TerminateReason.CANCELLED);


            @Test
            void should_succeed() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));

                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                taskManagementService.terminateTask(taskId, terminateInfo);

                assertEquals(CFTTaskState.CANCELLED, taskResource.getState());
                verify(camundaService, times(1)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
            }


            @Test
            void should_throw_exception_when_task_resource_not_found() {

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.empty());

                assertThatThrownBy(() -> taskManagementService.terminateTask(taskId, terminateInfo))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Resource not found");
                verify(camundaService, times(0)).deleteCftTaskState(taskId);
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }

    }
}
