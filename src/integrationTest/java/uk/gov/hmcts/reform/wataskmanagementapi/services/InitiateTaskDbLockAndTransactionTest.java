package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.OffsetDateTime;
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
import javax.persistence.EntityManager;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
public class InitiateTaskDbLockAndTransactionTest extends SpringBootIntegrationBaseTest {

    public static final String A_TASK_NAME = "follow Up Overdue Reasons For Appeal";
    public static final String A_TASK_TYPE = "followUpOverdueReasonsForAppeal";
    public static final String SOME_ASSIGNEE = "someAssignee";
    public static final String SOME_CASE_ID = "someCaseId";

    OffsetDateTime createdDate = OffsetDateTime.now();
    OffsetDateTime dueDate = createdDate.plusDays(1);
    String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

    private final InitiateTaskRequest initiateTaskRequest = new InitiateTaskRequest(
        InitiateTaskOperation.INITIATION,
        List.of(
            new TaskAttribute(TASK_TYPE, A_TASK_TYPE),
            new TaskAttribute(TASK_ASSIGNEE, SOME_ASSIGNEE),
            new TaskAttribute(TASK_CASE_ID, SOME_CASE_ID),
            new TaskAttribute(TASK_NAME, A_TASK_NAME),
            new TaskAttribute(TASK_DUE_DATE, formattedDueDate)
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
    private CftQueryService cftQueryService;
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
    private RoleAssignmentVerificationService roleAssignmentVerification;
    @Mock
    private EntityManager entityManager;

    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Mock
    private List<TaskOperationService> taskOperationServices;


    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
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


        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(
                           FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                           IDAM_USER_ID,
                           IDAM_USER_EMAIL
                       )
        ).thenReturn(true);

        testTaskResource = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, UNCONFIGURED, SOME_CASE_ID, dueDate);
        testTaskResource.setCreated(OffsetDateTime.now());
        assignedTask = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, ASSIGNED, SOME_CASE_ID, dueDate);
        assignedTask.setCreated(OffsetDateTime.now());

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
        inOrder.verify(configureTaskService).configureCFTTask(
            taskResourceCaptor.capture(),
            eq(new TaskToConfigure(taskId, A_TASK_TYPE, SOME_CASE_ID, A_TASK_NAME))
        );
        inOrder.verify(taskAutoAssignmentService).autoAssignCFTTask(any(TaskResource.class));
        inOrder.verify(camundaService).updateCftTaskState(any(), any());
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
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        AtomicReference<Future<TaskResource>> future1 = new AtomicReference<>();
        AtomicReference<Future<TaskResource>> future2 = new AtomicReference<>();

        transactionHelper.doInNewTransaction(
            () -> future1.set(executorService.submit(() -> taskManagementService.initiateTask(
                taskId,
                initiateTaskRequest
            ))));

        transactionHelper.doInNewTransaction(
            () -> future2.set(executorService.submit(() -> taskManagementService.initiateTask(
                taskId,
                initiateTaskRequest
            ))));

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
                "503, Database Conflict Error"
            ) && expectedSucceededCalls(futureResults, 1));
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
