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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.MARK_TO_RECONFIGURE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
class TaskReconfigurationControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";

    @MockBean
    private ClientAccessControlService clientAccessControlService;

    @SpyBean
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private String taskId;


    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
    }

    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_assigned() throws Exception {

        createTaskAndRoleAssignments(CFTTaskState.ASSIGNED, "caseId0");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId0")))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId0");
        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_were_already_marked() throws Exception {

        createTaskAndRoleAssignments(UNASSIGNED, "caseId2");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId2")))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
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
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> latestTaskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId2");
        latestTaskResources.forEach(task1 -> {
            TaskResource match = taskResources.stream().filter(task2 -> task1.getTaskId().equals(task2.getTaskId()))
                .findFirst().get();
            assertEquals(match.getReconfigureRequestTime(), task1.getReconfigureRequestTime());
        });
    }

    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_unassigned() throws Exception {

        createTaskAndRoleAssignments(UNASSIGNED, "caseId3");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId3")))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId3");
        taskResources.forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_status_is_not_active() throws Exception {

        createTaskAndRoleAssignments(CANCELLED, "caseId4");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(MARK_TO_RECONFIGURE, "caseId4")))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId4");
        taskResources.forEach(task -> {
            assertNull(task.getReconfigureRequestTime());
        });
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

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName, String caseId) {
        TaskOperation operation = new TaskOperation(operationName, UUID.randomUUID().toString());
        return new TaskOperationRequest(operation, taskFilters(caseId));
    }

    private List<TaskFilter<?>> taskFilters(String caseId) {
        TaskFilter<?> filter = new MarkTaskToReconfigureTaskFilter("case_id", List.of(caseId), TaskFilterOperator.IN);
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
