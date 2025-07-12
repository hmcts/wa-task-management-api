package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.launchdarkly.sdk.LDValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.GrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdDataService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DmnEvaluationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskReconfigurationTransactionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.EXECUTE_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.EXECUTE_RECONFIGURE_FAILURES;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages.MANDATORY_FIELD_MISSING_ERROR;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
@ExtendWith(OutputCaptureExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ExecuteReconfigurationTaskOperationControllerTest extends SpringBootIntegrationBaseTest {

    public static final String SYSTEM_USER_1 = "system_user1";
    public static final String ASSIGNEE_USER = "assigneeUser";
    public static final String OLD_ASSIGNEE_USER = "oldAssigneeUser";
    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    @MockitoBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;
    @MockitoBean
    private CftQueryService cftQueryService;
    @MockitoBean
    private CcdDataService ccdDataService;
    @MockitoBean
    private DmnEvaluationService dmnEvaluationService;
    @MockitoSpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockitoBean
    private RoleAssignmentService roleAssignmentService;
    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    @MockitoSpyBean
    TaskReconfigurationTransactionHandler taskReconfigurationTransactionHandler;
    @MockitoSpyBean
    TaskMandatoryFieldsValidator taskMandatoryFieldsValidator;

    private String taskId;
    private String bearerAccessToken1;

    public static void assertCloseTo(OffsetDateTime expected, OffsetDateTime actual, int offsetSeconds) {
        assertTrue(expected.minusSeconds(offsetSeconds).isBefore(actual) && expected.plusSeconds(offsetSeconds).isAfter(actual));
    }

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(List.of(
            new ConfigurationDmnEvaluationResponse(
                CamundaValue.stringValue("caseName"),
                CamundaValue.stringValue("Value"),
                CamundaValue.booleanValue(true)
            )
        ));
        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());
        CaseDetails caseDetails = new CaseDetails(
            "IA",
            "Asylum",
            SecurityClassification.PUBLIC.getSecurityClassification(),
            Map.of("caseAccessCategory", "categoryA,categoryC")
        );
        lenient().when(ccdDataService.getCaseData(anyString())).thenReturn(caseDetails);
        RoleAssignment roleAssignmentResource = buildRoleAssignment(
            ASSIGNEE_USER,
            "tribunalCaseworker",
            singletonList("IA")
        );
        List<RoleAssignment> roleAssignmentForAssignee = List.of(roleAssignmentResource);
        when(roleAssignmentService.getRolesByUserId(any())).thenReturn(roleAssignmentForAssignee);
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(roleAssignmentForAssignee);
    }

    @Test
    void should_execute_reconfigure_nextHearingDate_to_null_from_null() throws Exception {
        //create mock task
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        //mark to reconfigure
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        //assert nextHearingDate is null before reconfiguration
        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertNull(task.getNextHearingDate());
        });

        //call to update nextHearingDate to empty
        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        //execute reconfigure
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        //assert nextHearingDate is null after reconfiguration
        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesAfter.forEach(task -> {
            assertNull(task.getNextHearingDate());
        }
        );
    }

    @Test
    void should_execute_reconfigure_on_task_and_not_update_data_when_can_reconfigure_is_false() throws Exception {
        String caseIdToday = "caseId1-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResources.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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
            assertNotNull(task.getDueDateTime());
        });

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(false));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResources.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        Thread.sleep(5000);
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
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
            assertNotNull(task.getLastUpdatedTimestamp());
            assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
            assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
            assertNotNull(task.getDueDateTime());
        });
    }

    @Test
    void should_execute_reconfigure_on_task_and_update_data_when_can_reconfigure_is_true() throws Exception {
        String caseIdToday = "caseId2-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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
            assertNotNull(task.getDueDateTime());
        });

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        Thread.sleep(5000);
        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResourcesAfter
            .forEach(task -> {
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
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertEquals(ASSIGNEE_USER, task.getAssignee());
                assertEquals(CFTTaskState.ASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
            });
    }

    /*
     Scenario: Task reconfiguration with exception handling
     This test verifies that task reconfiguration is successful for the first and third tasks.
     It also ensures that if an exception occurs during the reconfiguration of the second task,
     the reconfiguration is retried for that task and rollback the changes if any for the second task.
    */
    @Test
    void should_rollback_changes_and_retry_task_reconfiguration_for_failed_task_when_any_exception_occurs() throws Exception {
        String caseIdToday = "caseId5-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        taskId = UUID.randomUUID().toString();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        taskId = UUID.randomUUID().toString();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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
            assertNotNull(task.getDueDateTime());
        });

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(eq(taskResourcesBefore.get(0).getTaskId()), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));
        String secondTaskId = taskResourcesBefore.get(1).getTaskId();
        //Throwing exception for second task while reassigning the task
        when(cftQueryService.getTask(eq(secondTaskId), any(), anyList())).thenThrow(new RuntimeException("Exception occurred while reassigning the task"));
        when(cftQueryService.getTask(eq(taskResourcesBefore.get(2).getTaskId()), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(2)));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        Thread.sleep(5000);
        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);


        taskResourcesAfter
            .stream().filter(taskResource -> !taskResource.getTaskId().equals(secondTaskId))
            .forEach(task -> {
                assertEquals(1, task.getMinorPriority());
                assertEquals(1, task.getMajorPriority());
                assertEquals("description", task.getDescription());
                assertEquals("TestCase", task.getCaseName());
                assertEquals("512401", task.getLocation());
                assertEquals("Manchester", task.getLocationName());
                assertEquals("caseCategory", task.getCaseCategory());
                assertEquals("routine_work", task.getWorkTypeResource().getId());
                assertEquals("JUDICIAL", task.getRoleCategory());
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertEquals(ASSIGNEE_USER, task.getAssignee());
                assertEquals(CFTTaskState.ASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertNull(task.getReconfigureRequestTime());
                assertNotNull(task.getLastReconfigurationTime());
            });
        TaskResource task = taskResourcesAfter.stream().filter(
            taskResource -> taskResource.getTaskId().equals(secondTaskId)).findFirst().orElseThrow();
        assertAll(
            () -> assertEquals(ASSIGNEE_USER, task.getAssignee()),
            () -> assertEquals(CFTTaskState.ASSIGNED, task.getState()),
            () -> assertNotNull(task.getReconfigureRequestTime()),
            () -> assertNull(task.getLastReconfigurationTime()),
            () -> assertEquals(OffsetDateTime.now().toLocalDate(), task.getReconfigureRequestTime().toLocalDate()),
            () -> assertNull(task.getMinorPriority()),
            () -> assertNull(task.getMajorPriority()),
            () -> assertNull(task.getDescription()),
            () -> assertNull(task.getCaseName()),
            () -> assertEquals("765324", task.getLocation()),
            () -> assertEquals("Taylor House", task.getLocationName()),
            () -> assertNull(task.getCaseCategory()),
            () -> assertNull(task.getWorkTypeResource()),
            () -> assertNull(task.getRoleCategory()),
            () -> assertEquals(OffsetDateTime.now().toLocalDate(), task.getPriorityDate().toLocalDate()),
            () -> assertNull(task.getNextHearingDate()),
            () -> assertNull(task.getNextHearingId()),
            () -> assertNotNull(task.getDueDateTime())
        );
        verify(taskReconfigurationTransactionHandler, times(4)).reconfigureTaskResource(secondTaskId);
    }

    @Test
    void should_not_execute_reconfigure_on_task_and_update_data_when_state_not_assigned_or_unassigned(
        CapturedOutput output) throws Exception {

        String caseIdToday = "caseId2-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
        });

        taskResourcesBefore.get(0).setState(CFTTaskState.CANCELLED);
        cftTaskDatabaseService.saveTask(taskResourcesBefore.get(0));

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());

        OffsetDateTime reconfigureDateTime = OffsetDateTime.now().minusSeconds(30L);
        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any()))
            .thenReturn(taskResourcesBefore.stream().map(TaskResource::getTaskId).toList());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(reconfigureDateTime)
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );


        Thread.sleep(5000);
        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResourcesAfter
            .forEach(task -> {
                assertEquals(CFTTaskState.CANCELLED, task.getState());
            });
        assertTrue(output.getOut().contains("did not execute reconfigure for Task Resource: taskId: " + taskId));
    }

    @Test
    void should_execute_reconfigure_on_task_and_update_title_when_can_reconfigure_is_true() throws Exception {
        String caseIdToday = "caseId3-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
            assertEquals("title", task.getTitle());
            assertNotNull(task.getReconfigureRequestTime());
        });


        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(List.of(
                new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("updatedTitle"),
                        booleanValue(true)
                )));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .untilAsserted(() -> {
                List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

                taskResourcesAfter
                    .forEach(task -> {
                        assertNotNull(task.getLastReconfigurationTime());
                        assertNull(task.getReconfigureRequestTime());
                        assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                        assertEquals("updatedTitle", task.getTitle());
                        assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
                    });
            });
    }

    @Test
    void should_execute_reconfigure_autoassignment_unassigned_to_assigned() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, null, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
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

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        Thread.sleep(5000);
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
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertEquals(ASSIGNEE_USER, task.getAssignee());
                assertEquals(CFTTaskState.ASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertEquals(TaskAction.AUTO_ASSIGN.getValue(), task.getLastUpdatedAction());
            }
        );
    }

    @Test
    void should_execute_reconfigure_autoassignment_unassigned_to_unassigned() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, null, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
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

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );
        Thread.sleep(5000);
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
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertNull(task.getAssignee());
                assertEquals(CFTTaskState.UNASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
            }
        );
    }


    @Test
    void should_execute_reconfigure_set_indexed_true() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, null, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
                    taskResourcesAfter.forEach(task -> {
                        assertNotNull(task.getLastReconfigurationTime());
                        assertNull(task.getReconfigureRequestTime());
                        assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                        assertEquals(CFTTaskState.UNASSIGNED, task.getState());
                        assertTrue(task.getIndexed());
                        assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
                    });
                    return true;
                });
    }

    @Test
    void should_execute_reconfigure_set_indexed_assigned_true() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, null, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
                    taskResourcesAfter.forEach(task -> {
                        assertNotNull(task.getLastReconfigurationTime());
                        assertNull(task.getReconfigureRequestTime());
                        assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                        assertEquals(CFTTaskState.ASSIGNED, task.getState());
                        assertTrue(task.getIndexed());
                        assertEquals(TaskAction.AUTO_ASSIGN.getValue(), task.getLastUpdatedAction());
                    });
                    return true;
                });
    }

    @Test
    void should_execute_reconfigure_autoassignment_assigned_to_assigned_another_user() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, OLD_ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(OLD_ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        Thread.sleep(5000);

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
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15Z").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15Z").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertEquals(ASSIGNEE_USER, task.getAssignee());
                assertEquals(CFTTaskState.ASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertEquals(TaskAction.AUTO_UNASSIGN_ASSIGN.getValue(), task.getLastUpdatedAction());
            }
        );
    }

    @Test
    void should_execute_reconfigure_autoassignment_assigned_to_unassigned() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        String dueDateTimeCheck = OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        Thread.sleep(5000);

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
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getPriorityDate().toLocalDate()
                );
                assertEquals(
                    OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00").toLocalDate(),
                    task.getNextHearingDate().toLocalDate()
                );
                assertEquals("nextHearingId1", task.getNextHearingId());
                assertNull(task.getAssignee());
                assertEquals(CFTTaskState.UNASSIGNED, task.getState());
                assertNotNull(task.getLastUpdatedTimestamp());
                assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                assertEquals(TaskAction.AUTO_UNASSIGN.getValue(), task.getLastUpdatedAction());
                assertCloseTo(dueDateTime, task.getDueDateTime(), 2);
            }
        );
    }

    @Test
    void should_not_execute_reconfigure_for_past_reconfigure_request_time() throws Exception {

        String caseIdToday = "caseId" + OffsetDateTime.now();
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))

        ).andExpectAll(
            status().is(HttpStatus.OK.value())
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
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().plusDays(1))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        Thread.sleep(5000);

        taskResources = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        taskResources.forEach(task -> {
            assertNull(task.getLastReconfigurationTime());
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    @Test
    void should_not_execute_reconfigure_if_task_validation_fails(CapturedOutput output) throws Exception {
        String jsonString = "{\"jurisdictions\":[\"WA\"]}";
        lenient().when(launchDarklyFeatureFlagProvider.getJsonValue(any(), any()))
            .thenReturn(LDValue.parse(jsonString));
        String caseIdToday = "caseId" + OffsetDateTime.now();
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))

        ).andExpectAll(
            status().is(HttpStatus.OK.value())
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
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .untilAsserted(() -> {
                List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
                taskResourcesAfter.forEach(task -> {
                    assertNull(task.getLastReconfigurationTime());
                    assertNotNull(task.getReconfigureRequestTime());
                });
                assertTrue(output.getOut()
                               .contains(MANDATORY_FIELD_MISSING_ERROR.getDetail() + taskResources.get(0).getTaskId()));
            });
    }

    @Test
    void should_execute_reconfigure_on_task_and_fail_due_to_calendar_configuration_alerts_captured(CapturedOutput output)
        throws Exception {
        String caseIdToday = "calendarCaseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        taskResourcesBefore.forEach(task -> {
            assertEquals(ASSIGNEE_USER, task.getAssignee());
            assertEquals(CFTTaskState.ASSIGNED, task.getState());
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
            assertNotNull(task.getDueDateTime());
        });

        List<ConfigurationDmnEvaluationResponse> caseDataConfigurationDmnResponse = configurationDmnResponse(true);
        List<ConfigurationDmnEvaluationResponse> calendarConfigurationDmnResponse = invalidIntermediateDateCalendarDmnResponse();
        List<ConfigurationDmnEvaluationResponse> configurationDmnResponse = new ArrayList<>();
        configurationDmnResponse.addAll(caseDataConfigurationDmnResponse);
        configurationDmnResponse.addAll(calendarConfigurationDmnResponse);

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(configurationDmnResponse);
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusMinutes(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE_FAILURES,
                    executeTaskFilters(OffsetDateTime.now().minusMinutes(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        OffsetDateTime retryWindow = OffsetDateTime.now().minusHours(0);

        List<TaskResource> taskResourcesAfter = cftTaskDatabaseService
            .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(
                List.of(CFTTaskState.ASSIGNED, CFTTaskState.UNASSIGNED), retryWindow);

        taskResourcesAfter.stream().filter(task -> task.getCaseId().equals(caseIdToday))
            .forEach(task -> {
                assertNull(task.getLastReconfigurationTime());
                assertNotNull(task.getReconfigureRequestTime());
            });

        String failureLogMessage = taskResourcesAfter.stream()
            .map(task -> "\n" + task.getTaskId()
                         + ", " + task.getTaskName()
                         + ", " + task.getState()
                         + ", " + task.getReconfigureRequestTime()
                         + ", " + task.getLastReconfigurationTime())
            .collect(Collectors.joining());
        assertTrue(output.getOut().contains("Task Execute Reconfiguration Failed for following tasks "
                                            + failureLogMessage));
    }

    private List<ConfigurationDmnEvaluationResponse> invalidIntermediateDateCalendarDmnResponse() {
        ConfigurationDmnEvaluationResponse calculatedDates = ConfigurationDmnEvaluationResponse.builder()
            .name(stringValue("calculatedDates"))
            .value(stringValue("nextHearingDate,nonSpecifiedIntDate,hearingDatePreDate,dueDate,priorityDate"))
            .build();
        LocalDateTime nextHearingDateLocalDateTime = LocalDateTime.of(
            2022,
            10,
            13,
            18,
            0,
            0
        );
        String nextHearingDateValue = nextHearingDateLocalDateTime
            .plusDays(1)
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        ConfigurationDmnEvaluationResponse nextHearingDate = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("nextHearingDate"))
            .value(CamundaValue.stringValue(nextHearingDateValue))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("hearingDatePreDateOriginRef"))
            .value(CamundaValue.stringValue("nonSpecifiedIntDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateIntervalDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateIntervalDays"))
            .value(CamundaValue.stringValue("5"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse hearingDatePreDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse
            .builder()
            .name(CamundaValue.stringValue("hearingDatePreDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateOriginRef = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateOriginRef"))
            .value(CamundaValue.stringValue("nextHearingDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateIntervalDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateIntervalDays"))
            .value(CamundaValue.stringValue("21"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingCalendar = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingCalendar"))
            .value(CamundaValue.stringValue("https://www.gov.uk/bank-holidays/england-and-wales.json"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateNonWorkingDaysOfWeek = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateNonWorkingDaysOfWeek"))
            .value(CamundaValue.stringValue("SATURDAY,SUNDAY"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateSkipNonWorkingDays = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateSkipNonWorkingDays"))
            .value(CamundaValue.stringValue("true"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse dueDateMustBeWorkingDay = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("dueDateMustBeWorkingDay"))
            .value(CamundaValue.stringValue("Next"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        ConfigurationDmnEvaluationResponse priorityDateOriginEarliest = ConfigurationDmnEvaluationResponse.builder()
            .name(CamundaValue.stringValue("priorityDateOriginLatest"))
            .value(CamundaValue.stringValue("hearingDatePreDate,dueDate"))
            .canReconfigure(CamundaValue.booleanValue(true))
            .build();

        return List.of(
            calculatedDates, nextHearingDate, hearingDatePreDateOriginRef,
            hearingDatePreDateIntervalDays, hearingDatePreDateNonWorkingCalendar,
            hearingDatePreDateNonWorkingDaysOfWeek, hearingDatePreDateSkipNonWorkingDays,
            hearingDatePreDateMustBeWorkingDay, dueDateOriginRef, dueDateIntervalDays,
            dueDateNonWorkingCalendar, dueDateMustBeWorkingDay, dueDateNonWorkingDaysOfWeek,
            dueDateSkipNonWorkingDays, priorityDateOriginEarliest
        );
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType operationName, List<TaskFilter<?>> taskFilters) {
        TaskOperation operation = TaskOperation
            .builder()
            .type(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(60)
            .retryWindowHours(0)
            .build();
        return new TaskOperationRequest(operation, taskFilters);
    }

    private List<TaskFilter<?>> executeTaskFilters(OffsetDateTime reconfigureRequestTime) {
        TaskFilter<?> filter = new ExecuteReconfigureTaskFilter("reconfigure_request_time",
            reconfigureRequestTime, TaskFilterOperator.AFTER
        );
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
                                     String taskId,
                                     CFTTaskState cftTaskState,
                                     String assignee,
                                     OffsetDateTime dueDateTime,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(dueDateTime);
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);
        taskResource.setAssignee(assignee);
        taskResource.setTitle("title");
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void createTaskAndRoleAssignments(CFTTaskState cftTaskState, String assignee, String caseId,
                                              OffsetDateTime dueDateTime) {

        //assigner permission : manage, own, cancel
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, true, false,
            new String[]{"IA"}, 1, true,
            TestRolesWithGrantType.SPECIFIC_TRIBUNAL_CASE_WORKER.getRoleCategory().name()
        );
        String jurisdiction = "IA";
        String caseType = "Asylum";
        insertDummyTaskInDb(jurisdiction, caseType, caseId, taskId, cftTaskState, assignee, dueDateTime,
            assignerTaskRoleResource
        );

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
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(1),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryA,categoryC")
            ),
            new PermissionsDmnEvaluationResponse(
                stringValue("seniorTribunalCaseworker"),
                stringValue("Read,Refer,Own,Execute,Manage,Cancel"),
                stringValue("IA"),
                integerValue(2),
                booleanValue(true),
                stringValue("LEGAL_OPERATIONS"),
                stringValue("categoryB,categoryD")
            )
        );
    }

    private List<ConfigurationDmnEvaluationResponse> configurationDmnResponse(boolean canReconfigure) {
        return asList(
            new ConfigurationDmnEvaluationResponse(stringValue("title"), stringValue("title1"),
                booleanValue(false)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("description"), stringValue("description"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseName"), stringValue("TestCase"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("region"), stringValue("1"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("location"), stringValue("512401"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("locationName"), stringValue("Manchester"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("caseManagementCategory"), stringValue("caseCategory"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("workType"), stringValue("routine_work"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("roleCategory"), stringValue("JUDICIAL"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("priorityDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("minorPriority"), stringValue("1"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("majorPriority"), stringValue("1"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(stringValue("nextHearingId"), stringValue("nextHearingId1"),
                booleanValue(canReconfigure)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("nextHearingDate"),
                stringValue("2021-05-09T20:15"),
                booleanValue(canReconfigure)
            )
        );
    }

    private RoleAssignment buildRoleAssignment(String actorId, String roleName, List<String> authorisations) {
        return RoleAssignment.builder()
            .id(UUID.randomUUID().toString())
            .actorIdType(ActorIdType.IDAM)
            .actorId(actorId)
            .roleName(roleName)
            .roleCategory(RoleCategory.LEGAL_OPERATIONS)
            .roleType(RoleType.ORGANISATION)
            .classification(Classification.PUBLIC)
            .authorisations(authorisations)
            .grantType(GrantType.STANDARD)
            .beginTime(LocalDateTime.now().minusYears(1))
            .endTime(LocalDateTime.now().plusYears(1))
            .build();
    }

}
