package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.launchdarkly.sdk.LDValue;
import org.junit.jupiter.api.BeforeAll;
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
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CcdDataService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.DmnEvaluationService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.TaskReconfigurationTransactionHandler;
import uk.gov.hmcts.reform.wataskmanagementapi.services.utils.TaskMandatoryFieldsValidator;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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

    TaskTestUtils taskTestUtils;

    private String bearerAccessToken1;

    public static void assertCloseTo(OffsetDateTime expected, OffsetDateTime actual, int offsetSeconds) {
        assertTrue(expected.minusSeconds(offsetSeconds).isBefore(actual) && expected.plusSeconds(offsetSeconds).isAfter(actual));
    }

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService);
    }

    @BeforeEach
    void setUp() {

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
        RoleAssignment roleAssignmentResource = taskTestUtils.buildRoleAssignment(
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        //mark to reconfigure
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
        )).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        //execute reconfigure
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(false));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResources.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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

    /**
     * This test verifies that when reconfigure is set to true and the internal field is set to reconfigure,
     * the internal field is ignored during the reconfiguration process and retains its existing value.
     */
    @Test
    void should_ignore_field_on_reconfigure_task_when_reconfigure_set_to_true_and_internal_field_set_to_reconfigure()
        throws Exception {
        String caseIdToday = "caseId4-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED,caseIdToday, dueDateTime, ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));
        lenient().when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(List.of(
            new ConfigurationDmnEvaluationResponse(
                stringValue("securityClassification"),
                stringValue("PRIVATE"),
                booleanValue(true)
            ),
            new ConfigurationDmnEvaluationResponse(
                stringValue("roleCategory"),
                stringValue("JUDICIAL"),
                booleanValue(true)
            )
        ));

        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
        assertEquals(1, taskResourcesBefore.size());
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

        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .untilAsserted(() -> {
                List<TaskResource> taskResourcesAfter = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);
                assertEquals(1, taskResourcesAfter.size());
                taskResourcesAfter.forEach(task -> {
                    assertNotNull(task.getLastReconfigurationTime());
                    assertNull(task.getReconfigureRequestTime());
                    assertTrue(LocalDateTime.now().isAfter(task.getLastReconfigurationTime().toLocalDateTime()));
                    assertEquals(ASSIGNEE_USER, task.getAssignee());
                    assertEquals(CFTTaskState.ASSIGNED, task.getState());
                    assertEquals("JUDICIAL", task.getRoleCategory());
                    assertNotNull(task.getLastUpdatedTimestamp());
                    assertEquals(SYSTEM_USER_1, task.getLastUpdatedUser());
                    assertEquals(TaskAction.CONFIGURE.getValue(), task.getLastUpdatedAction());
                    assertEquals(SecurityClassification.PUBLIC, task.getSecurityClassification());
                });
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(eq(taskResourcesBefore.get(0).getTaskId()), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));
        String secondTaskId = taskResourcesBefore.get(1).getTaskId();
        //Throwing exception for second task while reassigning the task
        when(cftQueryService.getTask(eq(secondTaskId), any(), anyList())).thenThrow(new RuntimeException("Exception occurred while reassigning the task"));
        when(cftQueryService.getTask(eq(taskResourcesBefore.get(2).getTaskId()), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(2)));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        final String taskId = taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime, ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());

        OffsetDateTime reconfigureDateTime = OffsetDateTime.now().minusSeconds(30L);
        when(cftTaskDatabaseService.getActiveTaskIdsAndReconfigureRequestTimeGreaterThan(
            anyList(), any()))
            .thenReturn(taskResourcesBefore.stream().map(TaskResource::getTaskId).toList());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(reconfigureDateTime)
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime, ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, caseIdToday, dueDateTime,null);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, caseIdToday, dueDateTime,null);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, caseIdToday, dueDateTime,null);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(taskTestUtils.permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, caseIdToday, dueDateTime,null);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE,
                                                                         taskTestUtils.markTaskFilters(caseIdToday))))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        List<TaskResource> taskResourcesBefore = cftTaskDatabaseService.findByCaseIdOnly(caseIdToday);

        when(dmnEvaluationService.evaluateTaskConfigurationDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString()
        )).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,OLD_ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE,
                                                                         taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE, taskTestUtils.markTaskFilters(caseIdToday))))
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
            anyString())).thenReturn(taskTestUtils.configurationDmnResponse(true));
        when(dmnEvaluationService.evaluateTaskPermissionsDmn(
            anyString(),
            anyString(),
            anyString(),
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(roleAssignmentService.queryRolesForAutoAssignmentByCaseId(any())).thenReturn(List.of());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE,
                                                                         taskTestUtils.markTaskFilters(caseIdToday))))

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
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().plusDays(1))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE,
                                                                         taskTestUtils.markTaskFilters(caseIdToday))))

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
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
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
        taskTestUtils.createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, caseIdToday, dueDateTime,ASSIGNEE_USER);
        doNothing().when(taskMandatoryFieldsValidator).validate(any(TaskResource.class));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(MARK_TO_RECONFIGURE,
                                                                         taskTestUtils.markTaskFilters(caseIdToday))))
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

        List<ConfigurationDmnEvaluationResponse> caseDataConfigurationDmnResponse =
            taskTestUtils.configurationDmnResponse(true);
        List<ConfigurationDmnEvaluationResponse> calendarConfigurationDmnResponse =
            taskTestUtils.invalidIntermediateDateCalendarDmnResponse();
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
            anyString())).thenReturn(taskTestUtils.permissionsResponse());
        when(cftQueryService.getTask(any(), any(), anyList())).thenReturn(Optional.of(taskResourcesBefore.get(0)));

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusMinutes(30L))
                )))
        ).andExpectAll(
            status().is(HttpStatus.OK.value())
        );

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskTestUtils.taskOperationRequest(
                    EXECUTE_RECONFIGURE_FAILURES,
                    taskTestUtils.executeTaskFilters(OffsetDateTime.now().minusMinutes(30L))
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

}
