package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationIdamStubConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationSecurityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.utils.CancellationProcessValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskOperationPerformService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AwaitilityIntegrationTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.WA_CASE_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.WA_JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
@ExtendWith(OutputCaptureExtension.class)
@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Import({AwaitilityIntegrationTestConfig.class, IntegrationSecurityTestConfig.class, IntegrationIdamStubConfig.class})
class TaskManagementServiceTest {

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private EntityManager entityManager;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoSpyBean
    private CamundaService camundaService;
    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;
    @Autowired
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    RoleAssignmentVerificationService roleAssignmentVerificationService;
    @Autowired
    private CftQueryService cftQueryService;
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

    private TerminationProcessHelper terminationProcessHelper;
    @MockitoBean
    private ConfigureTaskService configureTaskService;
    @MockitoBean
    private TaskAutoAssignmentService taskAutoAssignmentService;
    @Autowired
    protected TransactionHelper transactionHelper;

    private RoleAssignmentVerificationService roleAssignmentVerification;
    private ServiceMocks mockServices;
    @MockitoBean
    private List<TaskOperationPerformService> taskOperationPerformServices;

    @MockitoBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @Autowired
    private IdamTokenGenerator systemUserIdamToken;
    @MockitoBean
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;
    RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();

    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    private CancellationProcessValidator cancellationProcessValidator;

    public static final String USER_WITH_CANCELLATION_FLAG_ENABLED = "wa-user-with-cancellation-process-enabled";

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        roleAssignmentVerification = new RoleAssignmentVerificationService(
            cftTaskDatabaseService,
            cftQueryService,
            cftSensitiveTaskEventLogsDatabaseService);
        cancellationProcessValidator = new CancellationProcessValidator(launchDarklyFeatureFlagProvider);
        terminationProcessHelper = new TerminationProcessHelper(
            camundaService,
            systemUserIdamToken,
            cancellationProcessValidator);
        taskManagementService = new TaskManagementService(
            camundaService,
            cftTaskDatabaseService,
            cftTaskMapper,
            configureTaskService,
            taskAutoAssignmentService,
            roleAssignmentVerification,
            entityManager,
            systemUserIdamToken,
            cftSensitiveTaskEventLogsDatabaseService,
            taskMandatoryFieldsValidator,
            terminationProcessHelper);
    }

    @BeforeEach
    void beforeEach() {
        taskId = UUID.randomUUID().toString();
        mockServices.mockServiceAPIs();
    }

    protected Map<String, CamundaVariable> createMockCamundaVariables() {

        Map<String, CamundaVariable> variables = new HashMap<>();
        variables.put("caseId", new CamundaVariable("00000", "String"));
        variables.put("caseName", new CamundaVariable("someCaseName", "String"));
        variables.put("caseTypeId", new CamundaVariable("someCaseType", "String"));
        variables.put("taskState", new CamundaVariable("configured", "String"));
        variables.put("location", new CamundaVariable("someStaffLocationId", "String"));
        variables.put("locationName", new CamundaVariable("someStaffLocationName", "String"));
        variables.put("securityClassification", new CamundaVariable("SC", "String"));
        variables.put("title", new CamundaVariable("some_title", "String"));
        variables.put("executionType", new CamundaVariable("some_executionType", "String"));
        variables.put("taskSystem", new CamundaVariable("some_taskSystem", "String"));
        variables.put("jurisdiction", new CamundaVariable("some_jurisdiction", "String"));
        variables.put("region", new CamundaVariable("some_region", "String"));
        variables.put("appealType", new CamundaVariable("some_appealType", "String"));
        variables.put("autoAssigned", new CamundaVariable("false", "Boolean"));
        variables.put("assignee", new CamundaVariable("uid", "String"));
        return variables;
    }

    protected CamundaTask createMockedUnmappedTask() {
        return new CamundaTask(
            "someCamundaTaskId",
            "someCamundaTaskName",
            IDAM_USER_ID,
            ZonedDateTime.now(),
            ZonedDateTime.now().plusDays(1),
            "someCamundaTaskDescription",
            "someCamundaTaskOwner",
            "someCamundaTaskFormKey",
            "someProcessInstanceId"
        );
    }

    void verifyTransactionWasRolledBack(String taskId, CFTTaskState cftTaskState) {
        transactionHelper.doInNewTransaction(() -> {
            //Find the task
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);

            assertNotNull(savedTaskResource);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(taskId, savedTaskResource.get().getTaskId());
            assertEquals("taskName", savedTaskResource.get().getTaskName());
            assertEquals("taskType", savedTaskResource.get().getTaskType());

            //Because transaction was rolled back
            assertEquals(cftTaskState, savedTaskResource.get().getState());
        });
    }

    void verifyTransactionTerminated(String taskId) {
        transactionHelper.doInNewTransaction(() -> {
            //Find the task
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);

            assertNotNull(savedTaskResource);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(taskId, savedTaskResource.get().getTaskId());
            assertEquals("taskName", savedTaskResource.get().getTaskName());
            assertEquals("taskType", savedTaskResource.get().getTaskType());
            assertEquals(TERMINATED, savedTaskResource.get().getState());
        });
    }

    private void verifyTransactionCancelledWithTerminationProcess(String taskId, String terminationProcess) {
        transactionHelper.doInNewTransaction(() -> {
            //Find the task
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);

            assertNotNull(savedTaskResource);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(taskId, savedTaskResource.get().getTaskId());
            assertEquals("taskName", savedTaskResource.get().getTaskName());
            assertEquals("taskType", savedTaskResource.get().getTaskType());
            assertEquals(CANCELLED, savedTaskResource.get().getState());
            assertEquals(terminationProcess, savedTaskResource.get().getTerminationProcess().getValue());
        });
    }

    private void createAndSaveTestTask(String taskId, CFTTaskState cftTaskState) {
        transactionHelper.doInNewTransaction(() -> {
            TaskResource taskResource = new TaskResource(
                taskId,
                "taskName",
                "taskType",
                cftTaskState,
                OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
            );
            taskResource.setCreated(OffsetDateTime.now());
            taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
            taskResource.setCaseId("CASE_ID");
            taskResource.setJurisdiction("WA");
            taskResource.setRegion("1");
            taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
            taskResource.setLocation("765324");
            taskResource.setCaseTypeId(WA_CASE_TYPE);

            taskResource.setTaskRoleResources(prepareTaskResources(taskId));
            taskResourceRepository.save(taskResource);
        });
    }

    private Set<TaskRoleResource> prepareTaskResources(String taskId) {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            true,
            true,
            true,
            true,
            null,
            0,
            true,
            RoleCategory.LEGAL_OPERATIONS.name(),
            taskId,
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            true
        );
        Set<TaskRoleResource> taskRoleResources = new HashSet<>();
        taskRoleResources.add(taskRoleResource);
        return taskRoleResources;

    }

    private RoleAssignmentRequest prepareRoleAssignmentRequest() {
        return RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(WA_JURISDICTION)
                    .caseType(WA_CASE_TYPE)
                    .region("1")
                    .caseId("CASE_ID")
                    .build()
            )
            .build();
    }

    private void createAndAssignTestTask(String taskId) {
        transactionHelper.doInNewTransaction(() -> {
            TaskResource taskResource = new TaskResource(
                taskId,
                "taskName",
                "taskType",
                ASSIGNED,
                OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
            );
            taskResource.setAssignee(IDAM_USER_ID);
            taskResource.setCreated(OffsetDateTime.now());
            taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
            taskResource.setCaseId("CASE_ID");
            taskResource.setJurisdiction("WA");
            taskResource.setRegion("1");
            taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
            taskResource.setLocation("765324");
            taskResource.setCaseTypeId(WA_CASE_TYPE);
            taskResource.setTaskRoleResources(prepareTaskResources(taskId));

            taskResourceRepository.save(taskResource);
        });
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {
        @Test
        void should_rollback_transaction_when_exception_occurs_calling_camunda_for_cancel_task() {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
            when(camundaServiceApi.searchHistory(any(), any()))
                .thenReturn(singletonList(new HistoryVariableInstance(
                    "someId",
                    CFT_TASK_STATE.value(),
                    "some state"
                )));
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            createAndSaveTestTask(taskId, UNCONFIGURED);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();

            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse, null)))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verifyTransactionWasRolledBack(taskId, UNCONFIGURED);

        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(value = {
            "ASSIGNED,",
            "UNASSIGNED,",
            "COMPLETED,",
            "CANCELLED,",
            "ASSIGNED,EXUI_USER_CANCELLATION",
            "UNASSIGNED,EXUI_USER_CANCELLATION",
            "COMPLETED,EXUI_USER_CANCELLATION",
            "CANCELLED,EXUI_USER_CANCELLATION"
        })
        void should_set_task_state_terminated_when_camunda_api_throws_an_exception_and_cft_task_state_is_not_terminated(
            String state, String termnationProcess) {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

            when(camundaService.isCftTaskStateExistInCamunda(taskId))
                .thenReturn(false);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            CFTTaskState cftTaskState = CFTTaskState.valueOf(state);

            createAndSaveTestTask(taskId, cftTaskState);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();

            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse, termnationProcess)
            );

            verifyTransactionTerminated(taskId);

        }

        @Test
        void should_no_change_in_cft_task_state_when_camunda_task_state_pending_termination() {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

            List<HistoryVariableInstance> historyVariableInstances = singletonList(new HistoryVariableInstance(
                "id",
                "cftTaskState",
                "pendingTermination"
            ));

            when(camundaServiceApi.searchHistory(any(), any()))
                .thenReturn(historyVariableInstances);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            createAndSaveTestTask(taskId, UNASSIGNED);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();

            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse, null)))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verifyTransactionWasRolledBack(taskId, UNASSIGNED);

        }

        @Test
        void should_set_termination_process_from_request_map_when_it_is_not_empty_for_cancel_task() {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
            when(camundaServiceApi.searchHistory(any(), any()))
                .thenReturn(singletonList(new HistoryVariableInstance(
                    "someId",
                    CFT_TASK_STATE.value(),
                    "some state"
                )));

            createAndSaveTestTask(taskId, UNCONFIGURED);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();

            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse, "EXUI_USER_CANCELLATION")
            );

            verifyTransactionCancelledWithTerminationProcess(taskId, "EXUI_USER_CANCELLATION");
        }

    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        @Test
        void should_rollback_transaction_when_exception_occurs_calling_camunda_for_complete_task() {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            createAndAssignTestTask(taskId);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse, null)))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. Unable to update task state to completed.");

            verifyTransactionWasRolledBack(taskId, ASSIGNED);

        }

        @Test
        void should_rollback_transaction_when_exception_occurs_calling_camunda_complete_for_complete_task() {

            List<RoleAssignment> roleAssignments = new ArrayList<>();

            RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

            roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

            UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
            AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

            createAndAssignTestTask(taskId);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse, null)))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete partially succeeded. "
                                + "The Task state was updated to completed, but the Task could not be completed.");

            verifyTransactionWasRolledBack(taskId, ASSIGNED);

        }
    }

    @Nested
    @DisplayName("completeTaskWithPrivilegeAndCompletionOptions()")
    class CompleteTaskWithPrivilegeAndCompletionOptions {

        @Nested
        @DisplayName("when assignAndComplete completion option is true")
        class AssignAndCompleteIsTrue {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                List<RoleAssignment> roleAssignments = new ArrayList<>();

                RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

                roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

                UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
                AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

                createAndAssignTestTask(taskId);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true),
                        null
                    )))
                    .isInstanceOf(TaskAssignAndCompleteException.class)
                    .hasNoCause()
                    .hasMessage(
                        "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                        + "The Task was assigned to the user making the request but the Task could not be "
                        + "completed.");

                verifyTransactionWasRolledBack(taskId, ASSIGNED);

            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                List<RoleAssignment> roleAssignments = new ArrayList<>();

                RoleAssignmentRequest roleAssignmentRequest = prepareRoleAssignmentRequest();

                roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

                UserInfo userInfo = UserInfo.builder().uid(IDAM_USER_ID).build();
                AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, roleAssignments);

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).completeTask(any(), any(), any());

                createAndAssignTestTask(taskId);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false),
                        null
                    )))
                    .isInstanceOf(TaskCompleteException.class)
                    .hasNoCause()
                    .hasMessage("Task Complete Error: Task complete partially succeeded. "
                                + "The Task state was updated to completed, but the Task could not be completed.");

                verifyTransactionWasRolledBack(taskId, ASSIGNED);
            }
        }
    }

    @Nested
    @DisplayName("terminateTask()")
    class TerminateTask {

        @Test
        @DisplayName("should_log_error_and_throw_exception_when_task_save_fails_after_camunda_update")
        void should_log_error_and_throw_exception_when_task_save_fails_after_camunda_update(CapturedOutput output) {
            String randomTaskId = UUID.randomUUID().toString();
            createAndAssignTestTask(randomTaskId);
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(randomTaskId);
            TaskResource taskResource = savedTaskResource.orElse(null);
            assertNotNull(taskResource);
            doThrow(new RuntimeException("Database error")).when(cftTaskDatabaseService).saveTask(taskResource);
            TerminateInfo terminateInfo = new TerminateInfo("deleted");
            assertThatThrownBy(() -> taskManagementService.terminateTask(
                    randomTaskId,
                    terminateInfo
                ))
                .isInstanceOf(RuntimeException.class);

            verify(camundaService, times(1)).deleteCftTaskState(randomTaskId);
            verify(cftTaskDatabaseService).saveTask(taskResource);

            await()
                .untilAsserted(
                    () -> assertTrue(
                        output.getOut()
                            .contains("Error occurred while terminating task with id: "
                                      + randomTaskId),
                        "Received log message: " + output.getOut()
                    )
                );

        }

        @Test
        @DisplayName("should_log_error_and_not_update_camunda_state_on_task_attribute_update_failure")
        void should_log_error_and_not_update_camunda_state_on_task_attribute_update_failure(CapturedOutput output) {
            final String randomTaskId = UUID.randomUUID().toString();
            createAndAssignTestTask(randomTaskId);
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(randomTaskId);
            TaskResource taskResource = savedTaskResource.orElse(null);
            assertNotNull(taskResource);
            TerminateInfo terminateInfo = null;
            assertThatThrownBy(() -> taskManagementService.terminateTask(
                randomTaskId,
                terminateInfo
            )).isInstanceOf(NullPointerException.class);

            verify(camundaService, never()).deleteCftTaskState(randomTaskId);
            verify(cftTaskDatabaseService, never()).saveTask(taskResource);

            await()
                .untilAsserted(
                    () -> assertTrue(
                        output.getOut()
                            .contains("Error occurred while terminating task with id: " + randomTaskId),
                        "Received log message: " + output.getOut()
                    )
                );

        }

        @Nested
        @DisplayName("when terminate reason is completed")
        class Completed {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
                when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).searchHistory(any(), any());

                createAndSaveTestTask(taskId, UNCONFIGURED);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("completed")
                    )))
                    .isInstanceOf(ServerErrorException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage("There was a problem when deleting the historic cftTaskState");

                verifyTransactionWasRolledBack(taskId, UNCONFIGURED);
            }
        }

        @Nested
        @DisplayName("when terminate reason is cancelled")
        class Cancelled {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                createAndSaveTestTask(taskId, UNCONFIGURED);
                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).searchHistory(any(), any());

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () ->
                        taskManagementService.terminateTask(
                            taskId,
                            new TerminateInfo("cancelled")
                        ))
                )
                    .isInstanceOf(ServerErrorException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage("There was a problem when deleting the historic cftTaskState");

                verifyTransactionWasRolledBack(taskId, UNCONFIGURED);
            }
        }

        @Nested
        @DisplayName("when terminate reason is deleted")
        class Deleted {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                createAndSaveTestTask(taskId, UNCONFIGURED);
                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).searchHistory(any(), any());

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () ->
                        taskManagementService.terminateTask(
                            taskId,
                            new TerminateInfo("deleted")
                        ))
                )
                    .isInstanceOf(ServerErrorException.class)
                    .hasCauseInstanceOf(FeignException.class)
                    .hasMessage("There was a problem when deleting the historic cftTaskState");

                verifyTransactionWasRolledBack(taskId, UNCONFIGURED);
            }
        }

        @Test
        void should_delete_camunda_task_when_task_id_not_found_in_db() {
            when(camundaServiceApi.searchHistory(any(), any()))
                .thenReturn(List.of(new HistoryVariableInstance(taskId, CFT_TASK_STATE.value(), "pendingTermination")
                ));

            assertTrue(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(taskId).isEmpty());

            AtomicBoolean success = new AtomicBoolean(false);
            transactionHelper.doInNewTransaction(
                () -> {
                    taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("completed")
                    );
                    success.set(true);
                }
            );

            assertTrue(success.get());
            verify(camundaServiceApi, atMostOnce()).searchHistory(any(), any());
            verify(camundaServiceApi, atMostOnce()).deleteVariableFromHistory(any(), any());
        }

        @Test
        @DisplayName("should_set_termination_process_when_termination_process_from_camunda_returns_value_and_flag_on")
        void should_set_termination_process_when_termination_process_from_camunda_returns_value_and_flag_on() {
            String taskId = UUID.randomUUID().toString();
            createAndAssignTestTask(taskId);
            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "id",
                "cancellationProcess",
                "CASE_EVENT_CANCELLATION"
            );

            when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
                .thenReturn(Optional.of(historyVariableInstance));
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), anyString(), anyString())).thenReturn(true);

            AtomicBoolean success = new AtomicBoolean(false);

            transactionHelper.doInNewTransaction(
                () -> {
                    taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("deleted")

                    );
                    success.set(true);

                }
            );
            assertTrue(success.get());
            verify(cftTaskDatabaseService, times(1)).saveTask(any());
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(TerminationProcess.EXUI_CASE_EVENT_CANCELLATION,
                         savedTaskResource.get().getTerminationProcess());
        }

        @Test
        @DisplayName("should_not_set_termination_process_when_task_already_cancelled_by_user")
        void should_not_set_termination_process_when_task_already_cancelled_by_user(CapturedOutput output) {
            String taskId = UUID.randomUUID().toString();
            createAndAssignTestTask(taskId);
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);
            savedTaskResource.ifPresent(task -> {
                task.setTerminationProcess(TerminationProcess.EXUI_USER_CANCELLATION);
                taskResourceRepository.save(task);
            });
            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "id",
                "cancellationProcess",
                "CASE_EVENT_CANCELLATION"
            );

            when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
                .thenReturn(Optional.of(historyVariableInstance));
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), anyString(), anyString())).thenReturn(true);

            AtomicBoolean success = new AtomicBoolean(false);

            transactionHelper.doInNewTransaction(
                () -> {
                    taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("deleted")

                    );
                    success.set(true);

                }
            );
            assertTrue(success.get());
            verify(cftTaskDatabaseService, times(1)).saveTask(any());
            savedTaskResource = taskResourceRepository.findById(taskId);
            assertTrue(savedTaskResource.isPresent());
            assertEquals(TerminationProcess.EXUI_USER_CANCELLATION,
                         savedTaskResource.get().getTerminationProcess());
            assertTrue(
                output.getOut()
                    .contains("Cannot update the termination process for a Case Event Cancellation since it has"
                                  + " already been cancelled by a User for task " + taskId));

        }

        @Test
        @DisplayName("should_not_set_termination_process_when_termination_process_from_camunda_returns_value_flag_off")
        void should_not_set_termination_process_when_termination_process_from_camunda_returns_value_and_flag_off() {
            String taskId = UUID.randomUUID().toString();
            createAndAssignTestTask(taskId);
            HistoryVariableInstance historyVariableInstance = new HistoryVariableInstance(
                "id",
                "cancellationProcess",
                "CASE_EVENT_CANCELLATION"
            );
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), anyString(), anyString())).thenReturn(false);
            when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
                .thenReturn(Optional.of(historyVariableInstance));
            AtomicBoolean success = new AtomicBoolean(false);

            transactionHelper.doInNewTransaction(
                () -> {
                    taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("completed")

                    );
                    success.set(true);

                }
            );
            assertTrue(success.get());
            verify(cftTaskDatabaseService, times(1)).saveTask(any());
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);
            assertTrue(savedTaskResource.isPresent());
            assertNull(savedTaskResource.get().getTerminationProcess());
        }

        @Test
        @DisplayName("should_not_set_termination_process_when_termination_process_from_camunda_returns_empty")
        void should_not_set_termination_process_when_termination_process_from_camunda_returns_empty_and_flag_on() {
            String taskId = UUID.randomUUID().toString();
            createAndAssignTestTask(taskId);
            UserInfo mockedUserInfo =
                UserInfo.builder().uid(IDAM_USER_ID).email(USER_WITH_CANCELLATION_FLAG_ENABLED).build();
            when(systemUserIdamToken.getUserInfo(anyString())).thenReturn(mockedUserInfo);
            when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), anyString(), anyString())).thenReturn(true);

            when(camundaService.getVariableFromHistory(taskId, "cancellationProcess"))
                .thenReturn(Optional.empty());
            AtomicBoolean success = new AtomicBoolean(false);

            transactionHelper.doInNewTransaction(
                () -> {
                    taskManagementService.terminateTask(
                        taskId,
                        new TerminateInfo("completed")

                    );
                    success.set(true);

                }
            );
            assertTrue(success.get());
            verify(cftTaskDatabaseService, times(1)).saveTask(any());
            Optional<TaskResource> savedTaskResource = taskResourceRepository.findById(taskId);
            assertTrue(savedTaskResource.isPresent());
            assertNull(savedTaskResource.get().getTerminationProcess());

        }
    }
}
