package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationTest;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.auth.role.RoleAssignmentService.TOTAL_RECORDS;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@IntegrationTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiFirstTaskControllerTest {

    private static final String CASE_TYPE_ID = "WaCaseType";

    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockitoBean
    private CamundaService camundaService;

    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        when(clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTHORIZATION_TOKEN, CASE_TYPE_ID))
            .thenReturn(true);
        when(authTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        when(roleAssignmentServiceApi.queryRoleAssignments(anyString(), anyString(), any(), any(), any()))
            .thenReturn(ResponseEntity.ok()
                            .header(TOTAL_RECORDS, "0")
                            .body(new RoleAssignmentResource(List.of())));
    }

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void should_create_api_first_task_and_mark_it_indexed() throws Exception {
        UUID externalTaskId = UUID.randomUUID();

        mockMvc.perform(
                post("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTaskRequest(externalTaskId)))
            .andExpectAll(
                status().isCreated(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath("$.external_task_id").value(externalTaskId.toString()),
                jsonPath("$.task_type").value("reviewTask"),
                jsonPath("$.case_id").value("1615817621013640"),
                jsonPath("$.case_type_id").value(CASE_TYPE_ID),
                jsonPath("$.state").value("UNASSIGNED"),
                jsonPath("$.indexed").value(true),
                jsonPath("$.title").value("Review task title"),
                jsonPath("$.task_role_resources", hasSize(1)),
                jsonPath("$.task_role_resources[0].role_name").value("tribunal-caseworker"),
                jsonPath("$.task_role_resources[0].read").value(true),
                jsonPath("$.task_role_resources[0].claim").value(true),
                jsonPath("$.task_role_resources[0].complete").value(true)
            );

        TaskResource savedTask = taskResourceRepository.findAll().iterator().next();
        assertThat(savedTask.getExternalTaskId()).isEqualTo(externalTaskId.toString());
        assertThat(savedTask.getState()).isEqualTo(CFTTaskState.UNASSIGNED);
        assertThat(savedTask.getIndexed()).isTrue();
        assertThat(savedTask.isCamundaTask()).isFalse();
    }

    @Test
    void should_return_no_content_when_create_hits_idempotency_key() throws Exception {
        UUID externalTaskId = UUID.randomUUID();

        mockMvc.perform(
                post("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTaskRequest(externalTaskId)))
            .andExpect(status().isCreated());

        mockMvc.perform(
                post("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTaskRequest(externalTaskId)))
            .andExpect(status().isNoContent());
    }

    @Test
    void should_forbid_create_when_service_lacks_case_type_access() throws Exception {
        when(clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTHORIZATION_TOKEN, CASE_TYPE_ID))
            .thenReturn(false);

        mockMvc.perform(
                post("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createTaskRequest(UUID.randomUUID())))
            .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
        "cancel, UNASSIGNED, deleted, EXUI_CASE_EVENT_CANCELLATION",
        "complete, ASSIGNED, completed, EXUI_CASE_EVENT_COMPLETION"
    })
    void should_terminate_api_first_tasks_without_deleting_camunda_state(String action,
                                                                         CFTTaskState initialState,
                                                                         String reason,
                                                                         TerminationProcess process) throws Exception {
        String taskId = UUID.randomUUID().toString();
        insertApiFirstTask(taskId, initialState);

        mockMvc.perform(
                post("/tasks/terminate")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(terminateTasksRequest(action, taskId)))
            .andExpect(status().isNoContent());

        TaskResource task = taskResourceRepository.findById(taskId).orElseThrow();
        assertThat(task.getState()).isEqualTo(CFTTaskState.TERMINATED);
        assertThat(task.getTerminationReason()).isEqualTo(reason);
        assertThat(task.getTerminationProcess()).isEqualTo(process);
        assertThat(task.getLastUpdatedUser()).isNotBlank();
        verify(camundaService, never()).deleteCftTaskState(taskId);
    }

    @Test
    void should_forbid_terminate_when_service_lacks_case_type_access() throws Exception {
        when(clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTHORIZATION_TOKEN, CASE_TYPE_ID))
            .thenReturn(false);

        mockMvc.perform(
                post("/tasks/terminate")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(terminateTasksRequest("cancel", UUID.randomUUID().toString())))
            .andExpect(status().isForbidden());
    }

    @Test
    void should_reconfigure_api_first_tasks_and_skip_non_reconfigurable_tasks() throws Exception {
        String reconfigurableTaskId = UUID.randomUUID().toString();
        String completedTaskId = UUID.randomUUID().toString();
        insertApiFirstTask(reconfigurableTaskId, CFTTaskState.UNASSIGNED);
        insertApiFirstTask(completedTaskId, CFTTaskState.COMPLETED);

        mockMvc.perform(
                put("/tasks/reconfigure")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reconfigureTasksRequest(reconfigurableTaskId, completedTaskId)))
            .andExpectAll(
                status().isOk(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath("$.tasks", hasSize(1)),
                jsonPath("$.tasks[0].task_id").value(reconfigurableTaskId),
                jsonPath("$.tasks[0].title").value("Reconfigured title"),
                jsonPath("$.tasks[0].case_type_id").value(CASE_TYPE_ID)
            );

        TaskResource reconfigured = taskResourceRepository.findById(reconfigurableTaskId).orElseThrow();
        TaskResource skipped = taskResourceRepository.findById(completedTaskId).orElseThrow();
        assertThat(reconfigured.getLastReconfigurationTime()).isNotNull();
        assertThat(skipped.getTitle()).isEqualTo("Original title");
    }

    @Test
    void should_forbid_reconfigure_when_service_lacks_case_type_access() throws Exception {
        when(clientAccessControlService.hasExclusiveCaseTypeAccess(SERVICE_AUTHORIZATION_TOKEN, CASE_TYPE_ID))
            .thenReturn(false);

        mockMvc.perform(
                put("/tasks/reconfigure")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(reconfigureTasksRequest(UUID.randomUUID().toString(), UUID.randomUUID().toString())))
            .andExpect(status().isForbidden());
    }

    private String createTaskRequest(UUID externalTaskId) {
        return """
            {
              "task": {
                "external_task_id": "%s",
                "type": "reviewTask",
                "name": "Review task",
                "title": "Review task title",
                "created": "2026-01-10T08:45:00Z",
                "execution_type": "Manual",
                "case_id": "1615817621013640",
                "case_type_id": "%s",
                "case_category": "caseCategory",
                "case_name": "A test case",
                "jurisdiction": "WA",
                "region": "1",
                "location": "765324",
                "work_type": "decision_making_work",
                "role_category": "JUDICIAL",
                "security_classification": "PUBLIC",
                "due_date_time": "2026-01-20T10:15:00Z",
                "permissions": [
                  {
                    "role_name": "tribunal-caseworker",
                    "permissions": ["Read", "Claim", "Complete"]
                  }
                ]
              }
            }
            """.formatted(externalTaskId, CASE_TYPE_ID);
    }

    private String terminateTasksRequest(String action, String taskId) {
        return """
            {
              "action": "%s",
              "case_type_id": "%s",
              "task_ids": ["%s"]
            }
            """.formatted(action, CASE_TYPE_ID, taskId);
    }

    private String reconfigureTasksRequest(String reconfigurableTaskId, String skippedTaskId) {
        return """
            {
              "case_type_id": "%s",
              "tasks": [
                {
                  "id": "%s",
                  "title": "Reconfigured title",
                  "description": "Reconfigured description"
                },
                {
                  "id": "%s",
                  "title": "Should be skipped"
                }
              ]
            }
            """.formatted(CASE_TYPE_ID, reconfigurableTaskId, skippedTaskId);
    }

    private void insertApiFirstTask(String taskId, CFTTaskState state) {
        TaskResource task = new TaskResource(taskId, "Original task", "reviewTask", state);
        task.setExternalTaskId(UUID.randomUUID().toString());
        task.setCreated(OffsetDateTime.now());
        task.setDueDateTime(OffsetDateTime.now().plusDays(10));
        task.setTaskSystem(TaskSystem.SELF);
        task.setSecurityClassification(SecurityClassification.PUBLIC);
        task.setTitle("Original title");
        task.setExecutionTypeCode(new ExecutionTypeResource(
            ExecutionType.MANUAL, ExecutionType.MANUAL.getName(), ExecutionType.MANUAL.getDescription()
        ));
        task.setWorkTypeResource(new WorkTypeResource("decision_making_work"));
        task.setRoleCategory("JUDICIAL");
        task.setCaseId("1615817621013640");
        task.setCaseTypeId(CASE_TYPE_ID);
        task.setJurisdiction("WA");
        task.setRegion("1");
        task.setLocation("765324");
        task.setTaskRoleResources(Set.of(new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, false, false,
            new String[]{"WA"}, 1, false, "JUDICIAL", taskId, OffsetDateTime.now()
        )));
        cftTaskDatabaseService.saveTask(task);
    }
}
