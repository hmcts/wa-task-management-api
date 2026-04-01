package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import jakarta.persistence.EntityManager;
import lombok.Builder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TerminationProcess;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.WorkTypeResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTasksControllerTest {

    @MockitoBean
    private IdamWebApi idamWebApi;
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
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private WorkTypeResourceRepository workTypeResourceRepository;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void should_return_filtered_tasks_when_case_id_and_task_type_are_provided() throws Exception {
        String caseId = "1615817621013640";
        String matchingTaskId = UUID.randomUUID().toString();
        String assignee = UUID.randomUUID().toString();
        String taskName = "Review hearing bundle";
        String taskType = "reviewTask";
        String title = "Review hearing bundle title";
        String description = "Review the hearing bundle before listing";
        String location = "765324";
        String locationName = "Taylor House";
        String jurisdiction = "WA";
        String region = "1";
        String caseTypeId = "WaCaseType";
        String caseCategory = "CategoryA";
        String caseName = "A test case name";
        String roleCategory = "JUDICIAL";
        String nextHearingId = "next-hearing-123";
        CFTTaskState taskState = CFTTaskState.UNASSIGNED;
        TaskSystem taskSystem = TaskSystem.SELF;
        SecurityClassification securityClassification = SecurityClassification.PUBLIC;
        boolean autoAssigned = true;
        boolean warnings = true;
        int minorPriority = 11;
        int majorPriority = 22;
        TerminationProcess terminationProcess = TerminationProcess.EXUI_USER_CANCELLATION;

        OffsetDateTime createdDate = OffsetDateTime.of(2026, 1, 10, 8, 45, 0, 0, ZoneOffset.UTC);
        OffsetDateTime dueDate = OffsetDateTime.of(2026, 1, 20, 10, 15, 0, 0, ZoneOffset.UTC);
        OffsetDateTime nextHearingDate = OffsetDateTime.of(2026, 1, 22, 11, 30, 0, 0, ZoneOffset.UTC);
        OffsetDateTime priorityDate = OffsetDateTime.of(2026, 1, 15, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime reconfigureRequestTime = OffsetDateTime.of(2026, 1, 11, 12, 5, 0, 0, ZoneOffset.UTC);
        OffsetDateTime lastReconfigurationTime = OffsetDateTime.of(2026, 1, 12, 14, 10, 0, 0, ZoneOffset.UTC);

        WorkTypeResource workType = new WorkTypeResource("decision_making_work", "Decision-making work");
        ExecutionTypeResource executionType =
            entityManager.getReference(ExecutionTypeResource.class, ExecutionType.MANUAL);
        Map<String, String> additionalProperties = Map.of(
            "applicant", "Jane Citizen",
            "caseRef", "REF-0001"
        );

        insertTask(TaskData.builder()
                       .taskId(matchingTaskId)
                       .caseId(caseId)
                       .assignee(assignee)
                       .taskName(taskName)
                       .taskType(taskType)
                       .title(title)
                       .description(description)
                       .location(location)
                       .locationName(locationName)
                       .jurisdiction(jurisdiction)
                       .region(region)
                       .caseTypeId(caseTypeId)
                       .caseCategory(caseCategory)
                       .caseName(caseName)
                       .roleCategory(roleCategory)
                       .nextHearingId(nextHearingId)
                       .taskState(taskState)
                       .taskSystem(taskSystem)
                       .securityClassification(securityClassification)
                       .autoAssigned(autoAssigned)
                       .warnings(warnings)
                       .minorPriority(minorPriority)
                       .majorPriority(majorPriority)
                       .terminationProcess(terminationProcess)
                       .createdDate(createdDate)
                       .dueDate(dueDate)
                       .nextHearingDate(nextHearingDate)
                       .priorityDate(priorityDate)
                       .reconfigureRequestTime(reconfigureRequestTime)
                       .lastReconfigurationTime(lastReconfigurationTime)
                       .workType(workType)
                       .executionType(executionType)
                       .additionalProperties(additionalProperties)
                       .build());
        insertMinimalTask(UUID.randomUUID().toString(), caseId, "someOtherTaskType");
        insertMinimalTask(UUID.randomUUID().toString(), "1615817621013641", taskType);

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN)).thenReturn(true);

        mockMvc.perform(
                get("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .param("case_id", caseId)
                    .param("task_types", "reviewTask")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                status().isOk(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath("$.total_records").value(1),
                jsonPath("$.tasks", hasSize(1)),
                jsonPath("$.tasks[0].id").value(matchingTaskId),
                jsonPath("$.tasks[0].name").value(taskName),
                jsonPath("$.tasks[0].assignee").value(assignee),
                jsonPath("$.tasks[0].type").value(taskType),
                jsonPath("$.tasks[0].task_state").value(taskState.name()),
                jsonPath("$.tasks[0].task_system").value(taskSystem.getValue()),
                jsonPath("$.tasks[0].security_classification").value(
                    securityClassification.getSecurityClassification()
                ),
                jsonPath("$.tasks[0].task_title").value(title),
                jsonPath("$.tasks[0].created_date").value(createdDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].due_date").value(dueDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].location_name").value(locationName),
                jsonPath("$.tasks[0].location").value(location),
                jsonPath("$.tasks[0].execution_type").value("MANUAL"),
                jsonPath("$.tasks[0].jurisdiction").value(jurisdiction),
                jsonPath("$.tasks[0].region").value(region),
                jsonPath("$.tasks[0].case_type_id").value(caseTypeId),
                jsonPath("$.tasks[0].case_id").value(caseId),
                jsonPath("$.tasks[0].case_category").value(caseCategory),
                jsonPath("$.tasks[0].case_name").value(caseName),
                jsonPath("$.tasks[0].auto_assigned").value(autoAssigned),
                jsonPath("$.tasks[0].warnings").value(warnings),
                jsonPath("$.tasks[0].case_management_category").value(caseCategory),
                jsonPath("$.tasks[0].work_type_id").value(workType.getId()),
                jsonPath("$.tasks[0].work_type_label").value(workType.getLabel()),
                jsonPath("$.tasks[0].description").value(description),
                jsonPath("$.tasks[0].role_category").value(roleCategory),
                jsonPath("$.tasks[0].additional_properties.applicant").value(additionalProperties.get("applicant")),
                jsonPath("$.tasks[0].additional_properties.caseRef").value(additionalProperties.get("caseRef")),
                jsonPath("$.tasks[0].next_hearing_id").value(nextHearingId),
                jsonPath("$.tasks[0].next_hearing_date")
                    .value(nextHearingDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].minor_priority").value(minorPriority),
                jsonPath("$.tasks[0].major_priority").value(majorPriority),
                jsonPath("$.tasks[0].priority_date").value(priorityDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].reconfigure_request_time")
                    .value(reconfigureRequestTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].last_reconfiguration_time")
                    .value(lastReconfigurationTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)),
                jsonPath("$.tasks[0].termination_process").value(terminationProcess.name())
            );
    }

    @Test
    void should_return_bad_request_when_no_query_parameters_are_provided() throws Exception {
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN)).thenReturn(true);

        mockMvc.perform(
                get("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                status().isBadRequest(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON),
                jsonPath("$.detail")
                    .value("Bad Request: At least one query/filter parameter must be included in the request.")
            );
    }

    @Test
    void should_return_empty_tasks_when_filters_match_no_tasks() throws Exception {
        insertMinimalTask(UUID.randomUUID().toString(), "1615817621013641", "someOtherTaskType");

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN)).thenReturn(true);

        mockMvc.perform(
                get("/tasks")
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .param("case_id", "72912937129")
                    .param("task_types", "reviewTask,completeTask")
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(
                status().isOk(),
                content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON),
                jsonPath("$.total_records").value(0),
                jsonPath("$.tasks", hasSize(0))
            );
    }

    private void insertMinimalTask(String taskId, String caseId, String taskType) {
        TaskResource task = new TaskResource(
            taskId,
            null,
            taskType,
            (CFTTaskState) null
        );
        task.setCaseId(caseId);
        task.setCreated(OffsetDateTime.now());
        task.setDueDateTime(OffsetDateTime.now());
        cftTaskDatabaseService.saveTask(task);
    }

    private void insertTask(TaskData taskData) {
        TaskResource task = new TaskResource(
            taskData.taskId,
            taskData.taskName,
            taskData.taskType,
            taskData.taskState
        );
        task.setAssignee(taskData.assignee);
        task.setTaskSystem(taskData.taskSystem);
        task.setSecurityClassification(taskData.securityClassification);
        task.setTitle(taskData.title);
        task.setCreated(taskData.createdDate);
        task.setDueDateTime(taskData.dueDate);
        task.setLocationName(taskData.locationName);
        task.setLocation(taskData.location);
        task.setExecutionTypeCode(taskData.executionType);
        task.setJurisdiction(taskData.jurisdiction);
        task.setRegion(taskData.region);
        task.setCaseTypeId(taskData.caseTypeId);
        task.setCaseId(taskData.caseId);
        task.setCaseCategory(taskData.caseCategory);
        task.setCaseName(taskData.caseName);
        task.setAutoAssigned(taskData.autoAssigned);
        task.setHasWarnings(taskData.warnings);
        task.setWorkTypeResource(taskData.workType);
        task.setDescription(taskData.description);
        task.setRoleCategory(taskData.roleCategory);
        task.setAdditionalProperties(taskData.additionalProperties);
        task.setNextHearingId(taskData.nextHearingId);
        task.setNextHearingDate(taskData.nextHearingDate);
        task.setMinorPriority(taskData.minorPriority);
        task.setMajorPriority(taskData.majorPriority);
        task.setPriorityDate(taskData.priorityDate);
        task.setReconfigureRequestTime(taskData.reconfigureRequestTime);
        task.setLastReconfigurationTime(taskData.lastReconfigurationTime);
        task.setTerminationProcess(taskData.terminationProcess);
        cftTaskDatabaseService.saveTask(task);
    }

    @Builder
    private static class TaskData {
        private String taskId;
        private String caseId;
        private String assignee;
        private String taskName;
        private String taskType;
        private String title;
        private String description;
        private String location;
        private String locationName;
        private String jurisdiction;
        private String region;
        private String caseTypeId;
        private String caseCategory;
        private String caseName;
        private String roleCategory;
        private String nextHearingId;
        private CFTTaskState taskState;
        private TaskSystem taskSystem;
        private SecurityClassification securityClassification;
        private Boolean autoAssigned;
        private Boolean warnings;
        private Integer minorPriority;
        private Integer majorPriority;
        private TerminationProcess terminationProcess;
        private OffsetDateTime createdDate;
        private OffsetDateTime dueDate;
        private OffsetDateTime nextHearingDate;
        private OffsetDateTime priorityDate;
        private OffsetDateTime reconfigureRequestTime;
        private OffsetDateTime lastReconfigurationTime;
        private WorkTypeResource workType;
        private ExecutionTypeResource executionType;
        private Map<String, String> additionalProperties;
    }

}
