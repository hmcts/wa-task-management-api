package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.HashSet;
import java.util.List;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_CANCELLATION_COMPLETION_FEATURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
public class InitiateTaskTest extends SpringBootIntegrationBaseTest {

    public static final String A_TASK_NAME = "aTaskName";
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @Autowired
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

    private final InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(
        InitiateTaskOperation.INITIATION,
        List.of(
            new TaskAttribute(TASK_TYPE, "aTaskType"),
            new TaskAttribute(TASK_ASSIGNEE, "someAssignee"),
            new TaskAttribute(TASK_NAME, A_TASK_NAME)
        )
    );

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ServiceMocks mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

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

        mockServices.mockServiceAPIs();

        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_CANCELLATION_COMPLETION_FEATURE,
                IDAM_USER_ID
            )
        ).thenReturn(true);

    }

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void given_initiate_task_is_called_when_error_then_rollback_db() {
        when(taskAutoAssignmentService.autoAssignCFTTask(any(TaskResource.class)))
            .thenThrow(new RuntimeException("some error"));

        assertThrows(RuntimeException.class, () -> transactionHelper
            .doInNewTransaction(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest)));

        transactionHelper.doInNewTransaction(() -> assertEquals(0, taskResourceRepository.count()));

        verify(cftTaskMapper).mapToTaskResource(taskId, initiateTaskRequest.getTaskAttributes());
        verify(cftTaskDatabaseService).saveTask(argThat((task) -> task.getTaskId().equals(taskId)));
        verifyNoMoreInteractions(cftTaskDatabaseService);
    }

    @Test
    void given_task_is_not_locked_when_initiated_task_is_called_then_it_succeeds() {
        taskManagementService.initiateTask(taskId, initiateTaskRequest);

        assertEquals(1, taskResourceRepository.count());
        assertEquals(
            A_TASK_NAME,
            taskResourceRepository.getByTaskId(taskId).orElseThrow().getTaskName()
        );
    }

    @Test
    //todo: this test requires more realistic testing, probably a FT, to confirm the save op locks the DB row.
    void given_multiple_task_initiate_calls_then_expect_one_to_succeed_and_one_to_fail() {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicReference<Future<TaskResource>> future1 = new AtomicReference<>();
        AtomicReference<Future<TaskResource>> future2 = new AtomicReference<>();

        transactionHelper.doInNewTransaction(() -> future1.set(executorService
            .submit(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))));

        transactionHelper.doInNewTransaction(() -> future2.set(executorService
            .submit(() -> taskManagementService.initiateTask(taskId, initiateTaskRequest))));

        List<Future<TaskResource>> futureResults = List.of(
            future1.get(),
            future2.get()
        );

        //expect one call to succeed and the another to fail
        await()
            .ignoreExceptions()
            .pollInterval(2, TimeUnit.SECONDS)
            .atMost(10, TimeUnit.SECONDS)
            .until(() -> expectedFailureCalls(futureResults,
                1,
                "DataIntegrityViolationException")
                         && expectedSucceededCalls(futureResults, 1));
    }

    @SuppressWarnings("SameParameterValue")
    private boolean expectedSucceededCalls(List<Future<TaskResource>> futureResults, int expectedSucceededCalls) {
        Set<TaskResource> oneTaskSucceedCondition = new HashSet<>();
        futureResults.forEach((fr) -> {
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
