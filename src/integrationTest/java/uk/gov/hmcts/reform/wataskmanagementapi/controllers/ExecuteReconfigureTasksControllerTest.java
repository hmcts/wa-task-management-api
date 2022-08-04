package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskToConfigure;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.CaseConfigurationProviderService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.ConfigureTaskService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.EXECUTE_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
class ExecuteReconfigureTasksControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    OffsetDateTime createdDate = OffsetDateTime.now();
    OffsetDateTime dueDate = createdDate.plusDays(1);
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @MockBean
    private ConfigureTaskService configureTaskService;
    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;

    @MockBean
    private CaseConfigurationProviderService caseConfigurationProviderService;

    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private String taskId;
    private TaskResource testTaskResource;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        lenient().when(caseConfigurationProviderService.evaluateConfigurationDmn(anyString(),
            any())).thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(
                    CamundaValue.stringValue("caseName"),
                    CamundaValue.stringValue("Value"),
                    CamundaValue.booleanValue(true)
                )
        ));

        testTaskResource = new TaskResource(taskId, "someTaskName", "someTaskType", CFTTaskState.ASSIGNED, "caseId101", dueDate);
        testTaskResource.setCreated(OffsetDateTime.now());

        when(configureTaskService.configureCFTTask(any(TaskResource.class), any(TaskToConfigure.class)))
            .thenReturn(testTaskResource);

        when(taskAutoAssignmentService.reAutoAssignCFTTask(any(TaskResource.class)))
            .thenReturn(testTaskResource);

    }

    @Test
    void should_successfully_mark_to_configure_and_then_execute_reconfigure_on_task() throws Exception {

        String caseIdToday = "caseId" + OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertTrue(LocalDate.now().equals(task.getReconfigureRequestTime().toLocalDate()));
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(EXECUTE_RECONFIGURE,
                                                           executeTaskFilters(OffsetDateTime.now().plusDays(1)))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minus(Duration.ofDays(1))))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNotNull(task.getLastReconfigurationTime());
            assertNull(task.getReconfigureRequestTime());
            await().timeout(5, SECONDS);
            assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
        });
    }

    @Test
    void should_not_execute_reconfigure_for_past_reconfigure_request_time() throws Exception {

        String caseIdToday = "caseId" + OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))

        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertTrue(LocalDate.now().equals(task.getReconfigureRequestTime().toLocalDate()));
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().plusDays(1)))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNull(task.getLastReconfigurationTime());
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName, List<TaskFilter<?>> taskFilters) {
        TaskOperation operation = new TaskOperation(operationName, UUID.randomUUID().toString(), 2, 120);
        return new TaskOperationRequest(operation, taskFilters);
    }

    private List<TaskFilter<?>> executeTaskFilters(OffsetDateTime reconfigureRequestTime) {
        TaskFilter<?> filter = new ExecuteReconfigureTaskFilter("reconfigure_request_time",
            reconfigureRequestTime, TaskFilterOperator.AFTER);
        return List.of(filter);
    }

    private List<TaskFilter<?>> markTaskFilters(String caseId) {
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of(caseId), TaskFilterOperator.IN
        );
        return List.of(filter);
    }

    private void insertDummyTaskInDb(String jurisdiction,
                                     String caseType,
                                     String caseId,
                                     String taskId, CFTTaskState cftTaskState,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void createTaskAndRoleAssignments(CFTTaskState cftTaskState, String caseId) {
        //assigner permission : manage, own, cancel
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, true, true, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );
        String jurisdiction = "IA";
        String caseType = "Asylum";
        insertDummyTaskInDb(jurisdiction, caseType, caseId, taskId, cftTaskState, assignerTaskRoleResource);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId(caseId)
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
    }

}
