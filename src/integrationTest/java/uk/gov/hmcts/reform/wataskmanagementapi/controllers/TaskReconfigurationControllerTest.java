package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
class TaskReconfigurationControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_BEING_TESTED = "/task/operation";

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockBean
    private AccessControlService accessControlService;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private UserInfo mockedSecondaryUserInfo;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    private ServiceMocks mockServices;
    private String taskId;
    private AccessControlResponse assignerAccessControlResponse;
    private AccessControlResponse assigneeAccessControlResponse;
    private RoleAssignmentResource assignerRoleAssignmentResource;
    private RoleAssignmentResource assigneeRoleAssignmentResource;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
        when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);

        when(mockedSecondaryUserInfo.getUid())
            .thenReturn(SECONDARY_IDAM_USER_ID);
        when(mockedSecondaryUserInfo.getEmail())
            .thenReturn(SECONDARY_IDAM_USER_EMAIL);
        mockServices.mockUserInfo();
        mockServices.mockSecondaryUserInfo();
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
    }

    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_assigned() throws Exception {

        createDummyTasksAndAssign("IA", "Asylum");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId1");
        taskResources.stream().forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_were_already_marked() throws Exception {

        createDummyTasksAndAssign("IA", "Asylum");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId1");
        taskResources.stream().forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> latestTaskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId1");
        latestTaskResources.stream().forEach(task1 -> {
            TaskResource match = taskResources.stream().filter(task2 -> task1.getTaskId().equals(task2.getTaskId()))
                .findFirst().get();
            assertEquals(match.getReconfigureRequestTime(), task1.getReconfigureRequestTime());
        });
    }


    @Test
    void should_perform_mark_to_reconfigure_if_tasks_status_is_unassigned() throws Exception {

        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, assignerTaskRoleResource);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId1");
        taskResources.stream().forEach(task -> {
            assertNotNull(task.getReconfigureRequestTime());
        });
    }

    @Test
    void should_not_perform_mark_to_reconfigure_if_tasks_status_is_not_active() throws Exception {

        createDummyTasksAndCancel("IA", "Asylum");

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        List<TaskResource> taskResources = cftTaskDatabaseService.findByCaseIdOnly("caseId1");
        taskResources.stream().forEach(task -> {
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
                .content(asJsonString(taskOperationRequest(TaskOperationName.MARK_TO_RECONFIGURE)))
        ).andExpectAll(
            status().is(HttpStatus.FORBIDDEN.value())
        );
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationName operationName) {
        TaskOperation operation = new TaskOperation(operationName, UUID.randomUUID().toString());
        return new TaskOperationRequest(operation, taskFilters());
    }

    private List<TaskFilter> taskFilters() {
        TaskFilter filter = new TaskFilter("case_id", List.of("caseId1"), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private void createDummyTasksAndAssign(String jurisdiction, String caseType) throws Exception {

        //assigner permission : manage
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assignerTaskRoleResource);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        //assignee permissions : own, execute
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_CASE_MANAGER.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_CASE_MANAGER.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_CASE_MANAGER)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);
        assigneeAccessControlResponse = new AccessControlResponse(mockedUserInfo, assigneeRoles);
        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);


        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(assignerAccessControlResponse);


        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        lenient().when(accessControlService.getRolesGivenUserId(SECONDARY_IDAM_USER_ID, IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(assigneeAccessControlResponse);


        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        String assignEndpoint = String.format("/task/%s/assign", taskId);
        mockMvc.perform(
            post(assignEndpoint)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    private void createDummyTasksAndCancel(String jurisdiction, String caseType) throws Exception {

        //assigner permission : manage, own, cancel
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, true, false, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assignerTaskRoleResource);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(assignerAccessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        String cancelEndpoint = String.format("/task/%s/cancel", taskId);
        mockMvc.perform(
                post(cancelEndpoint)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isNoContent());

        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertTrue(taskResource.isPresent());
        assertEquals(CANCELLED, taskResource.get().getState());
    }


    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId, TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("caseId1");

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

}

