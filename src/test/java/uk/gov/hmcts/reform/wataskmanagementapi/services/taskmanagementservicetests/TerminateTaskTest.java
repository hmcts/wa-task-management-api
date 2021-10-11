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
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TerminateReason;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    CFTTaskMapper cftTaskMapper;
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    
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
            launchDarklyFeatureFlagProvider,
            configureTaskService,
            taskAutoAssignmentService
        );

        taskId = UUID.randomUUID().toString();
    }

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

