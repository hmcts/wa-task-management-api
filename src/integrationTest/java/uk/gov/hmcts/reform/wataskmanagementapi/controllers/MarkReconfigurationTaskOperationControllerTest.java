package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserIdamTokenGeneratorInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MarkReconfigurationTaskOperationControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    public static final String SYSTEM_USER_1 = "system_user1";

    @MockitoBean
    private ClientAccessControlService clientAccessControlService;

    @MockitoBean
    private CaseConfigurationProviderService caseConfigurationProviderService;

    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @MockitoBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;

    @MockitoBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    TaskTestUtils taskTestUtils;

    private String bearerAccessToken1;

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService);
    }

    @BeforeEach
    void setUp() {
        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
        lenient().when(caseConfigurationProviderService.evaluateConfigurationDmn(
            anyString(),
            any()
        )).thenReturn(List.of(
            new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue("caseName"),
                CamundaValue.stringValue("Value"),
                CamundaValue.booleanValue(true)
            )
        ));
    }

    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_assigned() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, "caseId0",null,null);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId0")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId0");
        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertTrue(LocalDate.now().equals(task.getReconfigureRequestTime().toLocalDate()));
            assertNotNull(task.getLastUpdatedTimestamp());
            assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
            assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), task.getLastUpdatedAction());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_were_already_marked() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "caseId2",null,null);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId2")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId2");
        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId2")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> latestTaskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId2");
        latestTaskResources.forEach(task1 -> {
            TaskResource match = taskResources.stream().filter(task2 -> task1.getTaskId().equals(task2.getTaskId()))
                .findFirst().get();
            assertTrue(LocalDate.now().equals(match.getReconfigureRequestTime().toLocalDate()));
            assertEquals(match.getReconfigureRequestTime(), task1.getReconfigureRequestTime());
            assertEquals(match.getLastUpdatedTimestamp(), task1.getLastUpdatedTimestamp());
            assertEquals(SYSTEM_USER_1, task1.getLastUpdatedUser());
            assertEquals(match.getLastUpdatedUser(), task1.getLastUpdatedUser());
            assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), task1.getLastUpdatedAction());
            assertEquals(match.getLastUpdatedAction(), task1.getLastUpdatedAction());
        });
    }

    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_unassigned() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "caseId3",null,null);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId3")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId3");
        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertTrue(LocalDate.now().equals(task.getReconfigureRequestTime().toLocalDate()));
            assertNotNull(task.getLastUpdatedTimestamp());
            assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
            assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), task.getLastUpdatedAction());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_status_is_not_active() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(CANCELLED, "caseId4",null,null);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId4")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId4");
        assertNull(taskResources.get(0).getReconfigureRequestTime());
        assertNull(taskResources.get(0).getLastUpdatedTimestamp());
        assertNull(taskResources.get(0).getLastUpdatedUser());
        assertNull(taskResources.get(0).getLastUpdatedAction());
    }


    @Test
    void should_not_perform_mark_to_reconfigure_when_service_authorization_token_is_not_valid() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId5")))
        ).andExpectAll(
            status().is(HttpStatus.FORBIDDEN.value())
        );
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_no_tasks_found() throws Exception {

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId5")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> latestTaskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId5");
        assertEquals(0, latestTaskResources.size());
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_is_locked_by_another_process() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId6",null,null);

        List<TaskResource> taskResourcesTobeLocked = cftTaskDatabaseService.findByCaseIdOnly("caseId6");
        taskResourcesTobeLocked.stream().forEach(task -> {
            when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(task.getTaskId(),
                                                                                     List.of(ASSIGNED, UNASSIGNED)))
                .thenThrow(new OptimisticLockException());
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId6")))
        ).andExpectAll(
            status().is(HttpStatus.CONFLICT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId6");
        taskResources.stream().forEach(task -> {
            assertNull(task.getReconfigureRequestTime());
            assertNull(task.getLastUpdatedTimestamp());
            assertNull(task.getLastUpdatedUser());
            assertNull(task.getLastUpdatedAction());
        });
    }

    @Test
    void should_partially_perform_mark_to_reconfigure_when_one_of_task_is_locked_by_another_process() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId7",null,null);
        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "caseId7",null,null);

        List<TaskResource> taskResourcesTobeLocked = cftTaskDatabaseService.findByCaseIdOnly("caseId7");
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(0).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new OptimisticLockException());
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(1).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(1)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId7")))
        ).andExpectAll(
            status().is(HttpStatus.CONFLICT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId7");
        //task1
        assertNull(taskResources.get(0).getReconfigureRequestTime());
        assertNull(taskResources.get(0).getLastUpdatedTimestamp());
        assertNull(taskResources.get(0).getLastUpdatedUser());
        assertNull(taskResources.get(0).getLastUpdatedAction());

        //task2
        assertNotNull(taskResources.get(1).getReconfigureRequestTime());
        assertNotNull(taskResources.get(1).getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.get(1).getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.get(1).getLastUpdatedAction());

    }

    @Test
    void should_partially_perform_mark_to_reconfigure_when_some_of_tasks_failed_to_be_marked_to_reconfigure() throws Exception {

        //4 tasks
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId8",null,null);
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId8",null,null);
        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "caseId8",null,null);
        taskTestUtils.createTaskAndRoleAssignments(UNASSIGNED, "caseId8",null,null);

        //2 tasks failed, 2 tasks succeeded
        List<TaskResource> taskResourcesTobeLocked = cftTaskDatabaseService.findByCaseIdOnly("caseId8");
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(0).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(0)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(1).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new OptimisticLockException());
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(2).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(2)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(3).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new OptimisticLockException());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId8")))
        ).andExpectAll(
            status().is(HttpStatus.CONFLICT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId8");
        //task1
        assertNotNull(taskResources.stream()
                          .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(0).getTaskId()))
                          .findFirst().get().getReconfigureRequestTime());
        assertNotNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(0).getTaskId()))
            .findFirst().get().getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(0).getTaskId()))
            .findFirst().get().getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(0).getTaskId()))
            .findFirst().get().getLastUpdatedAction());
        //task2
        assertNull(taskResources.stream()
                       .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
                       .findFirst().get().getReconfigureRequestTime());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
            .findFirst().get().getLastUpdatedTimestamp());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
            .findFirst().get().getLastUpdatedUser());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
            .findFirst().get().getLastUpdatedAction());
        //task3
        assertNotNull(taskResources.stream()
                          .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(2).getTaskId()))
                          .findFirst().get().getReconfigureRequestTime());
        assertNotNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(2).getTaskId()))
            .findFirst().get().getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(2).getTaskId()))
            .findFirst().get().getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(2).getTaskId()))
            .findFirst().get().getLastUpdatedAction());
        //task4
        assertNull(taskResources.stream()
                       .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(3).getTaskId()))
                       .findFirst().get().getReconfigureRequestTime());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(3).getTaskId()))
            .findFirst().get().getLastUpdatedTimestamp());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
            .findFirst().get().getLastUpdatedUser());
        assertNull(taskResources.stream()
            .filter(task -> task.getTaskId().equals(taskResourcesTobeLocked.get(1).getTaskId()))
            .findFirst().get().getLastUpdatedAction());
    }

    @Test
    void should_retry_and_perform_mark_to_reconfigure_if_first_attempt_failed_to_mark_task_to_reconfigure() throws Exception {

        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId9",null,null);

        List<TaskResource> taskResourcesTobeLocked = cftTaskDatabaseService.findByCaseIdOnly("caseId9");
        taskResourcesTobeLocked.stream().forEach(task -> {
            when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
                task.getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
                .thenThrow(new OptimisticLockException())
                .thenReturn(Optional.of(taskResourcesTobeLocked.get(0)));
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId9")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId9");
        taskResources.stream().forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertNotNull(task.getLastUpdatedTimestamp());
            assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
            assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), task.getLastUpdatedAction());
        });
    }

    @Test
    void should_retry_and_perform_mark_to_reconfigure_when_first_attempt_failed_to_mark_multiple_tasks_to_be_reconfigurable() throws Exception {

        //4 tasks
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId10",null,null);
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId10",null,null);
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId10",null,null);
        taskTestUtils.createTaskAndRoleAssignments(ASSIGNED, "caseId10",null,null);

        //2 tasks failed, 2 tasks succeeded
        List<TaskResource> taskResourcesTobeLocked = cftTaskDatabaseService.findByCaseIdOnly("caseId10");
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(0).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(0)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(1).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new OptimisticLockException())
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(1)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(2).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(2)));
        when(cftTaskDatabaseService.findByIdAndStateInObtainPessimisticWriteLock(
            taskResourcesTobeLocked.get(3).getTaskId(), List.of(ASSIGNED, UNASSIGNED)))
            .thenThrow(new OptimisticLockException())
            .thenReturn(Optional.of(taskResourcesTobeLocked.get(3)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId10")))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId10");
        assertNotNull(taskResources.get(0).getReconfigureRequestTime());
        assertNotNull(taskResources.get(0).getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.get(0).getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.get(0).getLastUpdatedAction());
        assertNotNull(taskResources.get(1).getReconfigureRequestTime());
        assertNotNull(taskResources.get(1).getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.get(1).getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.get(1).getLastUpdatedAction());
        assertNotNull(taskResources.get(2).getReconfigureRequestTime());
        assertNotNull(taskResources.get(2).getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.get(2).getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.get(2).getLastUpdatedAction());
        assertNotNull(taskResources.get(3).getReconfigureRequestTime());
        assertNotNull(taskResources.get(3).getLastUpdatedTimestamp());
        assertEquals(SYSTEM_USER_1, taskResources.get(3).getLastUpdatedUser());
        assertEquals(TaskAction.MARK_FOR_RECONFIGURE.getValue(), taskResources.get(3).getLastUpdatedAction());
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType operationName, String caseId) {
        TaskOperation operation = TaskOperation.builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(2)
            .retryWindowHours(120)
            .build();
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private List<TaskFilter<?>> taskFilters(String caseId) {
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter("case_id", List.of(caseId), TaskFilterOperator.IN);
        return List.of(filter);
    }

}
