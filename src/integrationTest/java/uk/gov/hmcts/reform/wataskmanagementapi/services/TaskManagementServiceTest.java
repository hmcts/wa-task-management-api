package uk.gov.hmcts.reform.wataskmanagementapi.services;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.TerminateInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ServerErrorException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskAssignAndCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCancelException;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.TaskCompleteException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.persistence.EntityManager;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaHelpers.IDAM_USER_ID;

@Slf4j
class TaskManagementServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private EntityManager entityManager;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @Autowired
    private CamundaService camundaService;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private CFTTaskMapper cftTaskMapper;
    @Autowired
    RoleAssignmentVerificationService roleAssignmentVerificationService;
    @MockBean
    private CftQueryService cftQueryService;
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

    private RoleAssignmentVerificationService roleAssignmentVerification;
    private ServiceMocks mockServices;
    @MockBean
    private List<TaskOperationService> taskOperationServices;
    @MockBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

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
            systemUserIdamToken
        );

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
            taskResourceRepository.save(taskResource);
        });
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
            taskResourceRepository.save(taskResource);
        });
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {
        @Test
        void cancelTask_should_rollback_transaction_when_exception_occurs_calling_camunda() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

            when(camundaServiceApi.searchHistory(any(), any()))
                .thenReturn(singletonList(new HistoryVariableInstance(
                    "someId",
                    CFT_TASK_STATE.value(),
                    "some state"
                )));
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            createAndSaveTestTask(taskId, UNCONFIGURED);
            Optional<TaskResource> taskResource = taskResourceRepository.findById(taskId);
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(taskResource);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verifyTransactionWasRolledBack(taskId, UNCONFIGURED);

        }

        @ParameterizedTest(name = "{0}")
        @CsvSource(value = {
            "ASSIGNED",
            "UNASSIGNED",
            "COMPLETED",
            "CANCELLED"
        })
        void should_set_task_state_terminated_when_camunda_api_throws_an_exception_and_cft_task_state_is_not_terminated(
            String state) {

            CFTTaskState cftTaskState = CFTTaskState.valueOf(state);

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
            when(accessControlResponse.getUserInfo())
                .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
            Map<String, CamundaVariable> mockedVariables = createMockCamundaVariables();
            when(camundaService.getTaskVariables(taskId)).thenReturn(mockedVariables);

            when(camundaService.isCftTaskStateExistInCamunda(taskId))
                .thenReturn(null);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            Optional<TaskResource> taskResource = Optional.of(new TaskResource(
                taskId,
                "taskName",
                "taskType",
                cftTaskState,
                OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
            ));

            when(cftQueryService.getTask(any(), any(), any(PermissionRequirements.class)))
                .thenReturn(taskResource);

            createAndSaveTestTask(taskId, cftTaskState);

            transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse)
            );

            verifyTransactionTerminated(taskId);

        }

        @Test
        void should_no_change_in_cft_task_state_when_camunda_task_state_pending_termination() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
            when(accessControlResponse.getUserInfo())
                .thenReturn(UserInfo.builder().uid(IDAM_USER_ID).email(IDAM_USER_EMAIL).build());
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

            Optional<TaskResource> taskResource = Optional.of(new TaskResource(
                taskId,
                "taskName",
                "taskType",
                UNASSIGNED,
                OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
            ));

            when(cftQueryService.getTask(any(), any(), any(PermissionRequirements.class)))
                .thenReturn(taskResource);

            createAndSaveTestTask(taskId, UNASSIGNED);

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.cancelTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCancelException.class)
                .hasNoCause()
                .hasMessage("Task Cancel Error: Unable to cancel the task.");

            verifyTransactionWasRolledBack(taskId, UNASSIGNED);

        }

    }

    @Nested
    @DisplayName("completeTask()")
    class CompleteTask {
        @Test
        void completeTask_should_rollback_transaction_when_exception_occurs_calling_camunda() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

            createAndAssignTestTask(taskId);
            Optional<TaskResource> taskResource = taskResourceRepository.findById(taskId);
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(taskResource);
            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse)))
                .isInstanceOf(TaskCompleteException.class)
                .hasNoCause()
                .hasMessage("Task Complete Error: Task complete failed. Unable to update task state to completed.");

            verifyTransactionWasRolledBack(taskId, ASSIGNED);

        }

        @Test
        void completeTask_should_rollback_transaction_when_exception_occurs_calling_camunda_complete() {

            AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
            RoleAssignment roleAssignment = mock(RoleAssignment.class);
            when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
            List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
            when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
            when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

            createAndAssignTestTask(taskId);
            Optional<TaskResource> taskResource = taskResourceRepository.findById(taskId);
            when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                .thenReturn(taskResource);

            doThrow(FeignException.FeignServerException.class)
                .when(camundaServiceApi).completeTask(any(), any(), any());

            assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                () -> taskManagementService.completeTask(taskId, accessControlResponse)))
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

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

                createAndAssignTestTask(taskId);
                Optional<TaskResource> taskResource = taskResourceRepository.findById(taskId);
                when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                    .thenReturn(taskResource);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(true)
                    )))
                    .isInstanceOf(TaskAssignAndCompleteException.class)
                    .hasNoCause()
                    .hasMessage(
                        "Task Assign and Complete Error: Task assign and complete partially succeeded. "
                        + "The Task was assigned to the user making the request but the Task could not be completed.");

                verifyTransactionWasRolledBack(taskId, ASSIGNED);

            }

        }

        @Nested
        @DisplayName("when assignAndComplete completion option is false")
        class AssignAndCompleteIsFalse {

            @Test
            void should_rollback_transaction_when_exception_occurs_calling_camunda() {

                AccessControlResponse accessControlResponse = mock(AccessControlResponse.class);
                RoleAssignment roleAssignment = mock(RoleAssignment.class);
                when(roleAssignment.getRoleType()).thenReturn(RoleType.ORGANISATION);
                List<RoleAssignment> roleAssignments = singletonList(roleAssignment);
                when(accessControlResponse.getRoleAssignments()).thenReturn(roleAssignments);
                when(accessControlResponse.getUserInfo()).thenReturn(UserInfo.builder().uid(IDAM_USER_ID).build());

                doThrow(FeignException.FeignServerException.class)
                    .when(camundaServiceApi).completeTask(any(), any(), any());

                createAndAssignTestTask(taskId);
                Optional<TaskResource> taskResource = taskResourceRepository.findById(taskId);
                when(cftQueryService.getTask(anyString(), anyList(), any(PermissionRequirements.class)))
                    .thenReturn(taskResource);

                assertThatThrownBy(() -> transactionHelper.doInNewTransaction(
                    () -> taskManagementService.completeTaskWithPrivilegeAndCompletionOptions(
                        taskId,
                        accessControlResponse,
                        new CompletionOptions(false)
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
    }
}
