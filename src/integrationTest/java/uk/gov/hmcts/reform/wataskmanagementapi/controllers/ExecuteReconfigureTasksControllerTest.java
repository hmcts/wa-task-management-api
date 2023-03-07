package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.PermissionsDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.configuration.TaskConfigurationResults;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CaseConfigurationProviderService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.EXECUTE_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.booleanValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.integerValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
class ExecuteReconfigureTasksControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";
    public static final String SYSTEM_USER_1 = "system_user1";
    public static final String ASSIGNEE_USER = "assigneeUser";
    public static final String OLD_ASSIGNEE_USER = "oldAssigneeUser";

    @MockBean
    private ClientAccessControlService clientAccessControlService;

    @MockBean
    private CftQueryService cftQueryService;

    @MockBean
    private CaseConfigurationProviderService caseConfigurationProviderService;

    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @MockBean
    private RoleAssignmentService roleAssignmentService;

    @MockBean(name = "systemUserIdamInfo")
    UserIdamTokenGeneratorInfo systemUserIdamInfo;

    @MockBean
    private IdamWebApi idamWebApi;

    @Autowired
    private IdamTokenGenerator systemUserIdamToken;

    private String taskId;
    private String bearerAccessToken1;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
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
        bearerAccessToken1 = "Token" + UUID.randomUUID();
        when(idamWebApi.token(any())).thenReturn(new Token(bearerAccessToken1, "Scope"));
        when(idamWebApi.userInfo(any())).thenReturn(UserInfo.builder().uid(SYSTEM_USER_1).build());

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
    void should_execute_reconfigure_on_task_and_not_update_data_when_can_reconfigure_is_false() throws Exception {
        String caseIdToday = "caseId1-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);

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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(false),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);
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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);
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
            status().is(HttpStatus.NO_CONTENT.value())
        );

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

    @Test
    void should_execute_reconfigure_autoassignment_unassigned_to_assigned() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.UNASSIGNED, null, caseIdToday, dueDateTime);

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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);
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
    void should_execute_reconfigure_autoassignment_assigned_to_assigned_another_user() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, OLD_ASSIGNEE_USER, caseIdToday, dueDateTime);

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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().minusSeconds(30L))
                )))
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

    public static void assertCloseTo(OffsetDateTime expected, OffsetDateTime actual, int offsetSeconds) {
        assertTrue(expected.minusSeconds(offsetSeconds).isBefore(actual) && expected.plusSeconds(offsetSeconds).isAfter(actual));
    }

    @Test
    void should_execute_reconfigure_autoassignment_assigned_to_unassigned() throws Exception {
        String caseIdToday = "caseId-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        OffsetDateTime dueDateTime = OffsetDateTime.now();
        String dueDateTimeCheck = OffsetDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, ASSIGNEE_USER, caseIdToday, dueDateTime);

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

        TaskConfigurationResults results = new TaskConfigurationResults(
            emptyMap(),
            configurationDmnResponse(true),
            permissionsResponse()
        );
        when(caseConfigurationProviderService.getCaseRelatedConfiguration(anyString(), anyMap(), anyBoolean()))
            .thenReturn(results);
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
                .content(asJsonString(taskOperationRequest(
                    EXECUTE_RECONFIGURE,
                    executeTaskFilters(OffsetDateTime.now().plusDays(1))
                )))
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
        TaskOperation operation = TaskOperation
            .builder()
            .name(operationName)
            .runId(UUID.randomUUID().toString())
            .maxTimeLimit(2)
            .retryWindowHours(120)
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
