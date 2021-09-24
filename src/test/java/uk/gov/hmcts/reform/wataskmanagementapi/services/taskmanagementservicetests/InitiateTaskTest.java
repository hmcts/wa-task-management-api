package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;

@ExtendWith(MockitoExtension.class)
class InitiateTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "aTaskType";
    public static final String A_TASK_NAME = "aTaskName";
    public static final String CASE_ID = "aCaseId";
    private final InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(
        INITIATION,
        asList(
            new TaskAttribute(TASK_TYPE, A_TASK_TYPE),
            new TaskAttribute(TASK_NAME, A_TASK_NAME)
        )
    );
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
    TaskResource taskResource;

    @BeforeEach
    void setUp() {
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

        taskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID
        );
    }

    @Test
    void given_initiateTask_then_task_is_saved() {
        mockInitiateTaskDependencies();

        TaskResource unassignedTaskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNASSIGNED,
            CASE_ID
        );
        when(taskAutoAssignmentService.autoAssignCFTTask(any())).thenReturn(unassignedTaskResource);

        taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(cftTaskMapper).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
        verify(configureTaskService).configureCFTTask(
            eq(taskResource),
            ArgumentMatchers.argThat((taskToConfigure) -> taskToConfigure.equals(new TaskToConfigure(
                taskId,
                A_TASK_TYPE,
                CASE_ID,
                A_TASK_NAME
            )))
        );


        verify(taskAutoAssignmentService).autoAssignCFTTask(taskResource);

        verify(camundaService).updateCftTaskState(
            taskId,
            TaskState.UNASSIGNED
        );

        verify(cftTaskDatabaseService).saveTask(taskResource);
    }

    @Test
    void given_initiateTask_should_throw_exception_when_cannot_obtain_lock() {
        doThrow(new RuntimeException("some exception"))
            .when(cftTaskDatabaseService).insertAndLock(anyString());

        assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest)
        )
            .isInstanceOf(DatabaseConflictException.class)
            .hasNoCause()
            .hasMessage("Database Conflict Error: "
                        + "The action could not be completed because there was a conflict in the database.");
    }

    private void mockInitiateTaskDependencies() {
        lenient().when(cftTaskMapper.mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes()))
            .thenReturn(taskResource);

        lenient().when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
            .thenReturn(taskResource);

        lenient().when(taskAutoAssignmentService.autoAssignCFTTask(any(TaskResource.class))).thenReturn(taskResource);

        lenient().when(cftTaskDatabaseService.saveTask(any(TaskResource.class))).thenReturn(taskResource);
    }
}

