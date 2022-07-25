package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
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
class CompleteTaskWithPrivilegeAndCompletionOptionsTest extends CamundaHelpers {

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
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);


                TaskResource taskResource = spy(TaskResource.class);

                when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), asList(OWN, EXECUTE)))
                    .thenReturn(Optional.of(taskResource));
                when(taskResource.getState()).thenReturn(CFTTaskState.COMPLETED);
                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(true);
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                );
                boolean taskStateIsAssignededAlready = TaskState.ASSIGNED.value()
                    .equals(mockedVariables.get("taskState"));
                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, taskStateIsAssignededAlready);
            }

            @Test
            void should_succeed_and_feature_flag_is_off() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);
                when(permissionEvaluatorService.hasAccess(
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
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                );
                boolean taskStateIsAssignededAlready = TaskState.ASSIGNED.value()
                    .equals(mockedVariables.get("taskState"));
                verify(camundaService, times(1))
                    .assignAndCompleteTask(taskId, IDAM_USER_ID, taskStateIsAssignededAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_is_false_and_completion_options() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignment);
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);
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
                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), asList(OWN, EXECUTE)))
                    .thenReturn(Optional.empty());
                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(true);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(true)
                ))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Task Not Found Error: The task could not be found.");
                verify(camundaService, times(0)).assignAndCompleteTask(any(), any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_succeed_and_feature_flag_is_on() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);

                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                TaskResource taskResource = spy(TaskResource.class);

                when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                    .thenReturn(Optional.of(taskResource));
                when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);
                when(taskResource.getAssignee()).thenReturn(userInfo.getUid());
                when(cftQueryService.getTask(taskId,accessControlResponse.getRoleAssignments(), asList(OWN, EXECUTE)))
                    .thenReturn(Optional.of(taskResource));

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(true);
                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                );
                boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value()
                    .equals(mockedVariables.get("taskState"));
                assertEquals(CFTTaskState.COMPLETED, taskResource.getState());
                verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
                verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_succeed_and_feature_flag_is_off() {
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

                taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                );
                boolean taskStateIsCompletedAlready = TaskState.COMPLETED.value()
                    .equals(mockedVariables.get("taskState"));
                verify(camundaService, times(1)).completeTask(taskId, taskStateIsCompletedAlready);
            }

            @Test
            void should_throw_role_assignment_verification_exception_when_has_access_returns_false() {

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
                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(false);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                ))
                    .isInstanceOf(RoleAssignmentVerificationException.class)
                    .hasNoCause()
                    .hasMessage("Role Assignment Verification: "
                                + "The request failed the Role Assignment checks performed.");

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }

            @Test
            void should_throw_task_state_incorrect_exception_when_task_has_no_assignee() {

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

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
            }


            @Test
            void should_throw_exception_when_task_resource_not_found_and_feature_flag_is_on() {
                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                List<RoleAssignment> roleAssignment = singletonList(mock(RoleAssignment.class));

                final UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build();
                when(accessControlResponse.getUserInfo()).thenReturn(userInfo);

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(false);
                TaskResource taskResource = spy(TaskResource.class);

                when(launchDarklyFeatureFlagProvider.getBooleanValue(
                    RELEASE_2_ENDPOINTS_FEATURE,
                    IDAM_USER_ID,
                    IDAM_USER_EMAIL
                    )
                ).thenReturn(true);

                assertThatThrownBy(() -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                    taskId,
                    accessControlResponse,
                    new CompletionOptions(false)
                ))
                    .isInstanceOf(TaskNotFoundException.class)
                    .hasNoCause()
                    .hasMessage("Task Not Found Error: The task could not be found.");

                verify(camundaService, times(0)).completeTask(any(), anyBoolean());
                verify(cftTaskDatabaseService, times(0)).saveTask(taskResource);
            }
        }
    }

}

