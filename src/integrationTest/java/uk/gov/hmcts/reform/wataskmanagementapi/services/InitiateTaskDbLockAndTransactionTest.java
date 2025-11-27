package uk.gov.hmcts.reform.wataskmanagementapi.services;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTime.CAMUNDA_DATA_TIME_FORMATTER;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.DUE_DATE;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Slf4j
@Transactional//@Testcontainers
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class InitiateTaskDbLockAndTransactionTest {

    public static final String A_TASK_NAME = "follow Up Overdue Reasons For Appeal";
    public static final String A_TASK_TYPE = "followUpOverdueReasonsForAppeal";
    public static final String SOME_ASSIGNEE = "someAssignee";
    public static final String SOME_CASE_ID = "someCaseId";

    private final OffsetDateTime dueDate = OffsetDateTime.now().plusDays(1);
    private final String formattedDueDate = CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);
    private final Map<String, Object> taskAttributes = new HashMap<>(Map.of(
        TASK_NAME.value(), A_TASK_NAME,
        TASK_CASE_ID.value(), SOME_CASE_ID,
        DUE_DATE.value(), formattedDueDate,
        TASK_TYPE.value(), A_TASK_TYPE,
        TASK_ASSIGNEE.value(), SOME_ASSIGNEE
    ));

    private final InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, taskAttributes);

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private CamundaService camundaService;
    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @MockitoSpyBean
    private CftQueryService cftQueryService;
    @MockitoSpyBean
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    private TaskManagementService taskManagementService;
    private String taskId;
    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockitoBean
    private ConfigureTaskService configureTaskService;
    @MockitoBean
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
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;

    @Mock
    private List<TaskOperationPerformService> taskOperationPerformServices;
    @Mock
    private IdamTokenGenerator idamTokenGenerator;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            entityManager,
            idamTokenGenerator,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator);

        testTaskResource = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, UNCONFIGURED, SOME_CASE_ID, dueDate);
        testTaskResource.setCreated(OffsetDateTime.now());
        assignedTask = new TaskResource(taskId, A_TASK_NAME, A_TASK_TYPE, ASSIGNED, SOME_CASE_ID, dueDate);
        assignedTask.setCreated(OffsetDateTime.now());

        when(cftTaskMapper.mapToTaskResource(taskId, taskAttributes)).thenReturn(testTaskResource);

        when(taskAutoAssignmentService.performAutoAssignment(any(), any(TaskResource.class)))
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

        inOrder.verify(cftTaskMapper).mapToTaskResource(taskId, taskAttributes);
        inOrder.verify(taskAutoAssignmentService).performAutoAssignment(any(), any(TaskResource.class));
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
