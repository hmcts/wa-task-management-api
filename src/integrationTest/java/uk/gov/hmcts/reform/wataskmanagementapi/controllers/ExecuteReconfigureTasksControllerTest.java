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
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.CaseConfigurationProviderService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.services.TaskAutoAssignmentService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.EXECUTE_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
class ExecuteReconfigureTasksControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";

    @MockBean
    private ClientAccessControlService clientAccessControlService;

    @MockBean
    private TaskAutoAssignmentService taskAutoAssignmentService;

    @MockBean
    private CaseConfigurationProviderService caseConfigurationProviderService;

    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private String taskId;

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

        when(taskAutoAssignmentService.reAutoAssignCFTTask(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    void should_execute_reconfigure_on_task_and_not_update_data_when_can_reconfigure_is_false() throws Exception {

        String caseIdToday = "caseId1-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
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
            assertEquals(LocalDate.now(), task.getReconfigureRequestTime().toLocalDate());
            assertNotNull(task.getReconfigureRequestTime());
            assertEquals(OffsetDateTime.now().toLocalDate(), task.getReconfigureRequestTime().toLocalDate());
            assertNull(task.getMinorPriority());
            assertNull(task.getMajorPriority());
            assertNull(task.getDescription());
            assertNull(task.getCaseName());
            assertEquals("765324", task.getLocation());
            assertEquals("Taylor House", task.getLocationName());
            assertNull(task.getCaseCategory());
            assertNull(task.getWorkTypeResource());
            assertNull(task.getRoleCategory());
            assertEquals(OffsetDateTime.now().toLocalDate(), task.getPriorityDate().toLocalDate());
            assertNull(task.getNextHearingDate());
            assertNull(task.getNextHearingId());
        });

        TaskConfigurationResults results = new TaskConfigurationResults(emptyMap(),
            configurationDmnResponse(false), permissionsResponse());
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap())).thenReturn(results);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L)))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNotNull(task.getLastReconfigurationTime());
            assertNull(task.getReconfigureRequestTime());
            await().timeout(5, SECONDS);
            assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
            assertNull(task.getMinorPriority());
            assertNull(task.getMajorPriority());
            assertNull(task.getDescription());
            assertNull(task.getCaseName());
            assertEquals("765324", task.getLocation());
            assertEquals("Taylor House", task.getLocationName());
            assertNull(task.getCaseCategory());
            assertNull(task.getWorkTypeResource());
            assertNull(task.getRoleCategory());
            assertEquals(OffsetDateTime.now().toLocalDate(), task.getPriorityDate().toLocalDate());
            assertNull(task.getNextHearingDate());
            assertNull(task.getNextHearingId());
        });
    }

    @Test
    void should_execute_reconfigure_on_task_and_update_data_when_can_reconfigure_is_true() throws Exception {

        String caseIdToday = "caseId2-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResourcesBefore.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
            assertEquals(OffsetDateTime.now().toLocalDate(), task.getReconfigureRequestTime().toLocalDate());
            assertNull(task.getMinorPriority());
            assertNull(task.getMajorPriority());
            assertNull(task.getDescription());
            assertNull(task.getCaseName());
            assertEquals("765324", task.getLocation());
            assertEquals("Taylor House", task.getLocationName());
            assertNull(task.getCaseCategory());
            assertNull(task.getWorkTypeResource());
            assertNull(task.getRoleCategory());
            assertEquals(OffsetDateTime.now().toLocalDate(), task.getPriorityDate().toLocalDate());
            assertNull(task.getNextHearingDate());
            assertNull(task.getNextHearingId());
        });

        TaskConfigurationResults results = new TaskConfigurationResults(emptyMap(),
            configurationDmnResponse(true), permissionsResponse());
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap())).thenReturn(results);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L)))))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResourcesAfter.forEach(task -> {
                assertNotNull(task.getLastReconfigurationTime());
                assertNull(task.getReconfigureRequestTime());
                assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                assertEquals(1, task.getMinorPriority());
                assertEquals(1, task.getMajorPriority());
                assertEquals("description", task.getDescription());
                assertEquals("TestCase", task.getCaseName());
                assertEquals("512401", task.getLocation());
                assertEquals("Manchester", task.getLocationName());
                assertEquals("caseCategory", task.getCaseCategory());
                assertEquals("routine_work", task.getWorkTypeResource().getId());
                assertEquals("JUDICIAL", task.getRoleCategory());
                assertEquals(OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate());
                assertEquals(OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate());
                assertEquals("nextHearingId1", task.getNextHearingId());
            }
        );
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
            assertEquals(LocalDate.now(), task.getReconfigureRequestTime().toLocalDate());
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

    private List<PermissionsDmnEvaluationResponse> permissionsResponse() {
        return asList(
            new PermissionsDmnEvaluationResponse(
                stringValue("tribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Manage,Cancel"),
                null,
                null,
                null,
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            ));
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse(boolean canReconfigure) {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                booleanValue(false)),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("priorityDate"),
                stringValue("2021-05-09T20:15:45.345875+01:00"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                booleanValue(canReconfigure)),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15:45.345875+01:00"),
                booleanValue(canReconfigure))
        );
    }

}
