package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
public class InitiateTaskDbLockAndTransactionTest extends SpringBootIntegrationBaseTest {

    public static final String A_TASK_NAME = "aTaskName";
    public static final String A_TASK_TYPE = "aTaskType";
    public static final String SOME_ASSIGNEE = "someAssignee";
    public static final String SOME_CASE_ID = "someCaseId";
    private final InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(
        InitiateTaskOperation.INITIATION,
        List.of(
            new TaskAttribute(TASK_TYPE, A_TASK_TYPE),
            new TaskAttribute(TASK_ASSIGNEE, SOME_ASSIGNEE),
            new TaskAttribute(TASK_CASE_ID, SOME_CASE_ID),
            new TaskAttribute(TASK_NAME, A_TASK_NAME)
        )
    );
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private CamundaService camundaService;
    @MockBean
    private CamundaQueryBuilder camundaQueryBuilder;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @SpyBean
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    private TaskManagementService taskManagementService;
    private String taskId;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    @Autowired
    private TransactionHelper transactionHelper;
    @Captor
    private ArgumentCaptor<TaskResource> taskResourceCaptor;
    private TaskResource testTaskResource;
    private TaskResource assignedTask;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();

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

        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
            IDAM_USER_ID
            )
        ).thenReturn(true);

        testTaskResource = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, UNCONFIGURED, SOME_CASE_ID);
        assignedTask = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, ASSIGNED, SOME_CASE_ID);

        when(taskAutoAssignmentService.autoAssignCFTTask(any(TaskResource.class)))
            .thenReturn(assignedTask);

        when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
            .thenReturn(testTaskResource);
    }

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void given_initiate_task_is_called_when_error_then_only_skeleton_is_persisted_in_db() {
        when(taskAutoAssignmentService.autoAssignCFTTask(any(TaskResource.class)))
            .thenThrow(new RuntimeException("some error"));

        assertThrows(RuntimeException.class,
            () -> taskManagementService.initiateTask(taskId, initiateTaskRequest));

        assertEquals(1, taskResourceRepository.count());

        Optional<TaskResource> find = taskResourceRepository.findById(taskId);
        assertTrue(find.isPresent());
        assertEquals(UNCONFIGURED, find.get().getState());

        verify(cftTaskMapper).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
        verify(cftTaskDatabaseService).saveTask(argThat((task) -> task.getTaskId().equals(taskId)));
        verifyNoMoreInteractions(cftTaskDatabaseService);
    }

    @Test
    void given_task_is_not_locked_when_initiated_task_is_called_then_it_succeeds() {
        taskManagementService.initiateTask(taskId, initiateTaskRequest);

        InOrder inOrder = inOrder(
            cftTaskMapper,
            cftTaskDatabaseService,
            configureTaskService,
            taskAutoAssignmentService,
            camundaService,
            cftTaskDatabaseService
        );

        inOrder.verify(cftTaskMapper).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());

        inOrder.verify(cftTaskDatabaseService).saveTask(taskResourceCaptor.capture());
        assertTrue(expectTaskWithValues(taskResourceCaptor.getValue(), ASSIGNED));

        inOrder.verify(configureTaskService).configureCFTTask(
            taskResourceCaptor.capture(),
            eq(new TaskToConfigure(taskId, A_TASK_TYPE, null, A_TASK_NAME))
        );
        assertTrue(expectTaskWithValues(taskResourceCaptor.getValue(), ASSIGNED));

        inOrder.verify(taskAutoAssignmentService).autoAssignCFTTask(taskResourceCaptor.capture());
        assertTrue(expectTaskWithValues(taskResourceCaptor.getValue(), UNASSIGNED));

        inOrder.verify(camundaService).updateCftTaskState(taskId, TaskState.UNASSIGNED);

        inOrder.verify(cftTaskDatabaseService).saveTask(testTaskResource);

        //verify task is in the DB
        assertEquals(1, taskResourceRepository.count());
        assertEquals(
            A_TASK_NAME,
            taskResourceRepository.getByTaskId(taskId).orElseThrow().getTaskName()
        );
    }

    @Test
    void given_multiple_task_initiate_calls_then_expect_one_to_succeed_and_one_to_fail() {
        ExecutorService executorService = Executors.newFixedThreadPool(1);

        AtomicReference<Future<TaskResource>> future1 = new AtomicReference<>();
        AtomicReference<Future<TaskResource>> future2 = new AtomicReference<>();

        future1.set(executorService.submit(() -> taskManagementService.initiateTask(
            taskId,
            initiateTaskRequest
        )));

        future2.set(executorService.submit(() -> taskManagementService.initiateTask(
            taskId,
            initiateTaskRequest
        )));

        List<Future<TaskResource>> futureResults = List.of(
            future1.get(),
            future2.get()
        );

        //expect one call to succeed and the another to fail
        await()
            .ignoreExceptions()
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> expectedFailureCalls(
                futureResults,
                1,
                "ConstraintViolationException"
            )
                         && expectedSucceededCalls(futureResults, 1));
    }

    private boolean expectTaskWithValues(TaskResource actualTaskResource, CFTTaskState cftTaskState) {
        return actualTaskResource.getTaskId().equals(taskId)
               && actualTaskResource.getTaskName().equals(A_TASK_NAME)
               && actualTaskResource.getAssignee().equals(SOME_ASSIGNEE)
               && actualTaskResource.getState().equals(cftTaskState)
               && actualTaskResource.getTaskType().equals(A_TASK_TYPE);
    }

    @SuppressWarnings("SameParameterValue")
    private boolean expectedSucceededCalls(List<Future<TaskResource>> futureResults, int expectedSucceededCalls) {
        Set<TaskResource> oneTaskSucceedCondition = new HashSet<>();
        futureResults.forEach(fr -> {
            if (fr.isDone()) {
                try {
                    TaskResource task = fr.get();
                    log.info("result task: " + task);
                    oneTaskSucceedCondition.add(task);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });
        return oneTaskSucceedCondition.size() == expectedSucceededCalls;
    }

    @SuppressWarnings("SameParameterValue")
    private boolean expectedFailureCalls(List<Future<TaskResource>> futureResults,
                                         int expectedFailureCalls,
                                         String expectedFailureException) {
        return futureResults.stream().filter((fr) -> {
            Exception exception;
            try {
                fr.get();
                return false;
            } catch (Exception e) {
                exception = assertThrows(Exception.class, fr::get);

                assertThat(exception).hasMessageContaining(expectedFailureException);
                return true;
            }
        }).count() == expectedFailureCalls;
    }

}
