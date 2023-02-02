package uk.gov.hmcts.reform.wataskmanagementapi.services.taskmanagementservicetests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.DatabaseConflictException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.validation.CustomConstraintViolationException;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.RoleAssignmentVerificationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskManagementService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.TaskOperationService;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;

@ExtendWith(MockitoExtension.class)
class InitiateTaskTest extends CamundaHelpers {

    public static final String A_TASK_TYPE = "followUpOverdueReasonsForAppeal";
    public static final String A_TASK_NAME = "follow Up Overdue Reasons For Appeal";
    public static final String CASE_ID = "aCaseId";
    @Mock
    CamundaService camundaService;
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
    private IdamTokenGenerator idamTokenGenerator;

    @Spy
    @InjectMocks
    TaskAutoAssignmentService taskAutoAssignmentService;

    RoleAssignmentVerificationService roleAssignmentVerification;
    TaskManagementService taskManagementService;
    String taskId;
    TaskResource taskResource;
    private InitiateTaskRequestMap initiateTaskRequest;
    @Mock
    private EntityManager entityManager;

    @Mock
    private List<TaskOperationService> taskOperationServices;
    @Mock
    private UserInfo userInfo;

    @BeforeEach
    void setUp() {
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

        taskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID
        );

        Map<String, Object> taskAttributes = new HashMap<>();
        taskAttributes.put(TASK_TYPE.value(), A_TASK_TYPE);
        taskAttributes.put(TASK_NAME.value(), A_TASK_NAME);

        initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, taskAttributes);
    }

    @Test
    void given_initiateTask_then_task_is_saved() {
        OffsetDateTime dueDate = OffsetDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().put(DUE_DATE.value(), formattedDueDate);
        initiateTaskRequest.getTaskAttributes().put("taskId", taskId);

        TaskResource unassignedTaskResource = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNASSIGNED,
            CASE_ID
        );
        unassignedTaskResource.setDueDateTime(dueDate);

        mockInitiateTaskDependencies(unassignedTaskResource);
        doReturn(unassignedTaskResource).when(taskAutoAssignmentService).autoAssignCFTTask(any());

        lenient().when(cftTaskMapper.readDate(any(), any(CamundaVariableDefinition.class), any())).thenCallRealMethod();
        taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());

        verify(configureTaskService).configureCFTTask(
            eq(taskResource),
            ArgumentMatchers.argThat((taskToConfigure) -> taskToConfigure.equals(new TaskToConfigure(
                taskId,
                A_TASK_TYPE,
                CASE_ID,
                A_TASK_NAME,
                getTaskAttributesWithDueDateUpdate(dueDate)
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
        OffsetDateTime dueDate = OffsetDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().put(DUE_DATE.value(), formattedDueDate);
        initiateTaskRequest.getTaskAttributes().put("taskId", taskId);

        TaskResource taskWithAssignee = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID,
            "someUserId"
        );

        taskWithAssignee.setDueDateTime(dueDate);
        mockInitiateTaskDependencies(taskWithAssignee);

        when(configureTaskService.configureCFTTask(any(), any())).thenReturn(taskWithAssignee);
        doReturn(true).when(taskAutoAssignmentService).checkAssigneeIsStillValid(any(), eq("someUserId"));

        when(cftTaskMapper.readDate(any(), any(CamundaVariableDefinition.class), any())).thenCallRealMethod();

        when(cftTaskMapper.mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes()))
            .thenReturn(taskWithAssignee);

        TaskResource taskResource = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(taskAutoAssignmentService, never()).autoAssignCFTTask(any());
        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
        verify(configureTaskService).configureCFTTask(
            eq(taskResource),
            ArgumentMatchers.argThat((taskToConfigure) -> taskToConfigure.equals(new TaskToConfigure(
                taskId,
                A_TASK_TYPE,
                CASE_ID,
                A_TASK_NAME,
                getTaskAttributesWithDueDateUpdate(dueDate)
            )))
        );

        verify(camundaService).updateCftTaskState(
            taskId,
            TaskState.ASSIGNED
        );

        verify(cftTaskDatabaseService).saveTask(taskResource);
    }

    @NotNull
    private Map<String, Object> getTaskAttributesWithDueDateUpdate(OffsetDateTime dueDate) {
        Map<String, Object> taskAttributes = new HashMap<>(initiateTaskRequest.getTaskAttributes());
        taskAttributes.put(DUE_DATE.value(), dueDate);
        return taskAttributes;
    }

    @Test
    void given_initiateTask_with_previous_invalid_assignee_should_reassign() {
        OffsetDateTime dueDate = OffsetDateTime.now();
        String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
        initiateTaskRequest.getTaskAttributes().put(DUE_DATE.value(), formattedDueDate);
        initiateTaskRequest.getTaskAttributes().put("taskId", taskId);

        TaskResource taskWithAssignee = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            UNCONFIGURED,
            CASE_ID,
            "someUserId"
        );
        mockInitiateTaskDependencies(taskWithAssignee);


        TaskResource taskReassigned = new TaskResource(
            taskId,
            A_TASK_NAME,
            A_TASK_TYPE,
            ASSIGNED,
            CASE_ID,
            "anotherUserId"
        );
        taskWithAssignee.setDueDateTime(dueDate);
        taskReassigned.setDueDateTime(dueDate);

        when(configureTaskService.configureCFTTask(any(), any())).thenReturn(taskWithAssignee);
        doReturn(false).when(taskAutoAssignmentService).checkAssigneeIsStillValid(any(), eq("someUserId"));

        doReturn(taskReassigned).when(taskAutoAssignmentService).autoAssignCFTTask(any());

        lenient().when(cftTaskMapper.readDate(any(), any(CamundaVariableDefinition.class), any())).thenCallRealMethod();

        TaskResource taskResource = taskManagementService.initiateTask(taskId, initiateTaskRequest);

        verify(cftTaskMapper, atLeastOnce()).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());

        Map<String, Object> taskAttributes = getTaskAttributesWithDueDateUpdate(dueDate);
        verify(configureTaskService).configureCFTTask(
            eq(taskResource),
            ArgumentMatchers.argThat((taskToConfigure) -> taskToConfigure.equals(new TaskToConfigure(
                taskId,
                A_TASK_TYPE,
                CASE_ID,
                A_TASK_NAME,
                taskAttributes
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
        initiateTaskRequest.getTaskAttributes().put(DUE_DATE.value(), formattedDueDate);

        assertThatThrownBy(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest)
        )
            .isInstanceOf(DatabaseConflictException.class)
            .hasNoCause()
            .hasMessage("Database Conflict Error: "
                        + "The action could not be completed because there was a conflict in the database.");
    }

    @Test
    void should_set_task_attributes_when_initiate_task_request_initiated() {

        Map<String, Object> taskAttributes = initiateTaskRequest.getTaskAttributes();

        assertNotNull(taskAttributes);
        assertEquals(A_TASK_TYPE, taskAttributes.get(TASK_TYPE.value()));
        assertEquals(A_TASK_NAME, taskAttributes.get(TASK_NAME.value()));
    }


    private void mockInitiateTaskDependencies(TaskResource expected) {
        lenient().when(idamTokenGenerator.generate()).thenReturn("Bearer Token");
        lenient().when(idamTokenGenerator.getUserInfo(any())).thenReturn(userInfo);
        lenient().when(userInfo.getUid()).thenReturn("SYSTEM_USER_IDAM_ID");
        when(cftTaskMapper.mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes()))
            .thenReturn(expected);

        lenient().when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))

            .thenReturn(taskResource);
        lenient().doReturn(taskResource).when(taskAutoAssignmentService).autoAssignCFTTask(any());
        lenient().when(cftTaskDatabaseService.saveTask(any(TaskResource.class))).thenReturn(taskResource);
    }
}

