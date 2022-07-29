package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MarkTaskReconfigurationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_LOCATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class InitiateTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "followUpOverdueReasonsForAppeal";
    public static final String A_TASK_NAME = "follow Up Overdue Reasons For Appeal";
    public static final String CASE_ID = "aCaseId";
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
    @Spy
    CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
    @Mock
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    ConfigureTaskService configureTaskService;
    @Mock
    TaskAutoAssignmentService taskAutoAssignmentService;
    @Mock
    private MarkTaskReconfigurationService taskReconfigurationService;

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    TaskResource taskResource;
    private InitiateTaskRequest initiateTaskRequest;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Mock
    private List<TaskOperationService> taskOperationServices;


    @BeforeEach
    void setUp() {
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

        taskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID
        );

        List<TaskAttribute> taskAttributeList = new ArrayList<>();
        taskAttributeList.add(new TaskAttribute(TASK_TYPE, A_TASK_TYPE));
        taskAttributeList.add(new TaskAttribute(TASK_NAME, A_TASK_NAME));
        initiateTaskRequest = new InitiateTaskRequest(INITIATION, taskAttributeList);
    }

    @Test
    void given_initiateTask_then_task_is_saved() {
        ZonedDateTime dueDate = ZonedDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));
        mockInitiateTaskDependencies();

        TaskResource unassignedTaskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNASSIGNED,
            CASE_ID
        );

        when(taskAutoAssignmentService.autoAssignCFTTask(any())).thenReturn(unassignedTaskResource);
        when(cftTaskMapper.readDate(any(), any(), any())).thenCallRealMethod();
        taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
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
    void should_succeed_and_filter_out_nulls() {
        ZonedDateTime dueDate = ZonedDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));
        mockInitiateTaskDependencies();

        TaskResource unassignedTaskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNASSIGNED,
            CASE_ID
        );

        when(taskAutoAssignmentService.autoAssignCFTTask(any())).thenReturn(unassignedTaskResource);
        when(cftTaskMapper.readDate(any(), any(), any())).thenCallRealMethod();
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_LOCATION, null));

        TaskResource taskResource = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        assertNull(taskResource.getLocation());
        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
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
    void given_initiateTask_with_previous_valid_assignee_should_keep_assignee() {
        ZonedDateTime dueDate = ZonedDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));
        mockInitiateTaskDependencies();

        TaskResource taskWithAssignee = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID,
            "someUserId"
        );

        when(configureTaskService.configureCFTTask(any(), any())).thenReturn(taskWithAssignee);
        when(taskAutoAssignmentService.checkAssigneeIsStillValid(any(), eq("someUserId"))).thenReturn(true);

        when(cftTaskMapper.readDate(any(), any(), any())).thenCallRealMethod();

        TaskResource taskResource = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(taskAutoAssignmentService, never()).autoAssignCFTTask(any());
        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
        verify(configureTaskService).configureCFTTask(
            eq(taskResource),
            ArgumentMatchers.argThat((taskToConfigure) -> taskToConfigure.equals(new TaskToConfigure(
                taskId,
                A_TASK_TYPE,
                CASE_ID,
                A_TASK_NAME
            )))
        );

        verify(camundaService).updateCftTaskState(
            taskId,
            TaskState.ASSIGNED
        );

        verify(cftTaskDatabaseService).saveTask(taskResource);
    }

    @Test
    void given_initiateTask_with_previous_invalid_assignee_should_reassign() {
        ZonedDateTime dueDate = ZonedDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));
        mockInitiateTaskDependencies();

        TaskResource taskWithAssignee = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID,
            "someUserId"
        );


        TaskResource taskReassigned = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            ASSIGNED,
            CASE_ID,
            "anotherUserId"
        );

        when(configureTaskService.configureCFTTask(any(), any())).thenReturn(taskWithAssignee);
        when(taskAutoAssignmentService.checkAssigneeIsStillValid(any(), eq("someUserId"))).thenReturn(false);

        when(taskAutoAssignmentService.autoAssignCFTTask(any())).thenReturn(taskReassigned);

        when(cftTaskMapper.readDate(any(), any(), any())).thenCallRealMethod();

        TaskResource taskResource = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
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
            TaskState.ASSIGNED
        );

        verify(cftTaskDatabaseService).saveTask(taskResource);
    }

    @Test
    void should_throw_custom_constraint_exception_when_no_due_date() {

        assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest)
        )
            .isInstanceOf(CustomConstraintViolationException.class)
            .hasNoCause()
            .hasMessage("Constraint Violation");
    }

    @Test
    void given_initiateTask_when_cannot_get_lock_should_throw_exception() throws SQLException {
        doThrow(new SQLException("some sql exception"))
            .when(cftTaskDatabaseService).insertAndLock(anyString(), any());
        ZonedDateTime dueDate = ZonedDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().add(new TaskAttribute(TASK_DUE_DATE, formattedDueDate));

        assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest)
        )
            .isInstanceOf(DatabaseConflictException.class)
            .hasNoCause()
            .hasMessage("Database Conflict Error: "
                        + "The action could not be completed because there was a conflict in the database.");
    }

    @Test
    void should_set_task_attributes_when_initiate_task_request_initiated() {

        List<TaskAttribute> taskAttributes = initiateTaskRequest.getTaskAttributes();

        assertNotNull(taskAttributes);
        assertNotNull(taskAttributes.get(0));
        assertEquals(TASK_TYPE, taskAttributes.get(0).getName());
        assertEquals(A_TASK_TYPE, taskAttributes.get(0).getValue());

        assertNotNull(taskAttributes.get(1));
        assertEquals(TASK_NAME, taskAttributes.get(1).getName());
        assertEquals(A_TASK_NAME, taskAttributes.get(1).getValue());

    }

    private void mockInitiateTaskDependencies() {
        when(cftTaskMapper.mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes()))
            .thenReturn(taskResource);

        lenient().when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
            .thenReturn(taskResource);

        lenient().when(taskAutoAssignmentService.autoAssignCFTTask(any(TaskResource.class))).thenReturn(taskResource);

        lenient().when(cftTaskDatabaseService.saveTask(any(TaskResource.class))).thenReturn(taskResource);
    }
}

