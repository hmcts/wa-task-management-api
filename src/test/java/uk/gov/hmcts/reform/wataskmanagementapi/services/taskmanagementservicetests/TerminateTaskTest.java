package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
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
import java.util.Optional;
import java.util.UUID;
import javax.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TerminateTaskTest extends CamundaHelpers {

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
    @Mock
    private List<TaskOperationService> taskOperationServices;

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;


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
    @DisplayName("When Terminate Reason is Completed")
    class Completed {

        TerminateInfo terminateInfo = new TerminateInfo("completed");

        @Test
        void should_succeed() {

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.terminateTask(taskId, terminateInfo);

            assertEquals(CFTTaskState.TERMINATED, taskResource.getState());
            assertEquals("completed", taskResource.getTerminationReason());
            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }

        @Test
        void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            taskManagementService.terminateTask(taskId, terminateInfo);

            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
        }

    }

    @Nested
    @DisplayName("When Terminate Reason is Cancelled")
    class Cancelled {
        TerminateInfo terminateInfo = new TerminateInfo("cancelled");


        @Test
        void should_succeed() {

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.terminateTask(taskId, terminateInfo);

            assertEquals(CFTTaskState.TERMINATED, taskResource.getState());
            assertEquals("cancelled", taskResource.getTerminationReason());
            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }


        @Test
        void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            taskManagementService.terminateTask(taskId, terminateInfo);

            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
        }

    }

    @Nested
    @DisplayName("When Terminate Reason is Deleted")
    class Deleted {
        TerminateInfo terminateInfo = new TerminateInfo("deleted");

        @Test
        void should_succeed() {

            TaskResource taskResource = spy(TaskResource.class);

            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.of(taskResource));

            when(cftTaskDatabaseService.saveTask(taskResource)).thenReturn(taskResource);

            taskManagementService.terminateTask(taskId, terminateInfo);

            assertEquals(CFTTaskState.TERMINATED, taskResource.getState());
            assertEquals("deleted", taskResource.getTerminationReason());
            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(1)).saveTask(taskResource);
        }


        @Test
        void should_handle_when_task_resource_not_found_and_delete_task_in_camunda() {
            when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId))
                .thenReturn(Optional.empty());

            taskManagementService.terminateTask(taskId, terminateInfo);

            verify(camundaService, times(1)).deleteCftTaskState(taskId);
            verify(cftTaskDatabaseService, times(0)).saveTask(any(TaskResource.class));
        }

    }

}

