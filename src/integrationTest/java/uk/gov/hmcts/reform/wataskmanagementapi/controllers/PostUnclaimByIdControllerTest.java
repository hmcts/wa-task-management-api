package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL_GP;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID_GP;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostUnclaimByIdControllerTest extends SpringBootIntegrationBaseTest {
    private static final String ENDPOINT_PATH = "/task/%s/unclaim";
    private static String ENDPOINT_BEING_TESTED;
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
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockBean
    private AccessControlService accessControlService;
    private ServiceMocks mockServices;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private UserInfo mockedGPUserInfo;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_return_204_unclaim_successful() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("unclaimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing()
            .when(camundaServiceApi).unclaimTask(any(), any());
        doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isNoContent()
        );
    }

    @Test
    void should_return_500_with_application_problem_response_when_task_update_call_fails() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("unclaimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doThrow(FeignException.FeignServerException.class)
            .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-unclaim-error"),
                jsonPath("$.title").value("Task Unclaim Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Unclaim Error: Task unclaim failed. Unable to update task state to unassigned.")
            ));
    }

    @Test
    void should_return_500_with_application_problem_response_when_unclaim_call_fails() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("unclaimCaseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("IA", "Asylum", taskId, taskRoleResource);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
        doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).unclaimTask(any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-unclaim-error"),
                jsonPath("$.title").value("Task Unclaim Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Unclaim Error: Task unclaim partially succeeded. "
                    + "The Task state was updated to unassigned, but the Task could not be unclaimed.")
            ));
    }

    @Test
    public void user_should_unclaim_task_when_granular_permissions_are_off()
        throws Exception {
        String jurisdiction = "WA";
        String caseType = "caseType";

        mockServices.mockServiceAPIs();

        setUpMocks(IDAM_USER_ID, IDAM_USER_EMAIL, false);

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, false, false, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource, mockedUserInfo.getUid());


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        verify(camundaServiceApi).unclaimTask(any(), any());
        TaskResource taskResource = cftTaskDatabaseService.findByIdOnly(taskId).get();
        assertNull(taskResource.getAssignee());
        assertEquals(UNASSIGNED, taskResource.getState());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "true, false, true",
        "false, true, true",
        "false, true, false"
    })
    public void user_should_unclaim_task_when_grant_type_standard_with_gp_permissions_on_with_unclaim(
        boolean unclaimPermission, boolean unassignPermission, boolean useUserIdAsAssignee)
        throws Exception {

        mockServices.mockServiceAPIsGp();

        setUpMocks(IDAM_USER_ID_GP, IDAM_USER_EMAIL_GP, true);

        //CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        //when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        String jurisdiction = "WA";
        String caseType = "caseType";

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
            taskId, OffsetDateTime.now(),
            false, false, false, false, unclaimPermission, false, unassignPermission, false, false, false
        );
        String assignee;
        if (useUserIdAsAssignee) {
            assignee = mockedGPUserInfo.getUid();
        } else {
            assignee = "otheruserid";
        }
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource, assignee);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );

        verify(camundaServiceApi).unclaimTask(any(), any());
        TaskResource taskResource = cftTaskDatabaseService.findByIdOnly(taskId).get();
        assertNull(taskResource.getAssignee());
        assertEquals(UNASSIGNED, taskResource.getState());
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false, false, true",
        "true, false, false"
    })
    public void user_throw_403_when_unclaim_task_with_no_permissions_gp_permissions_on(
        boolean unclaimPermission, boolean unassignPermission, boolean useUserIdAsAssignee)
        throws Exception {

        mockServices.mockServiceAPIsGp();

        setUpMocks(IDAM_USER_ID_GP, IDAM_USER_EMAIL_GP, true);

        //CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        //when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        String jurisdiction = "WA";
        String caseType = "caseType";

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
            taskId, OffsetDateTime.now(),
            false, false, false, false, unclaimPermission, false, unassignPermission, false, false, false
        );
        String assignee;
        if (useUserIdAsAssignee) {
            assignee = mockedGPUserInfo.getUid();
        } else {
            assignee = "otheruserid";
        }
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource, assignee);


        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.FORBIDDEN.value())
        );

        TaskResource taskResource = cftTaskDatabaseService.findByIdOnly(taskId).get();
        assertEquals(assignee, taskResource.getAssignee());
        assertEquals(ASSIGNED, taskResource.getState());
    }

    private void setUpMocks(String idamUserId, String idamUserEmail, boolean gpOn) {
        when(mockedUserInfo.getUid())
            .thenReturn(idamUserId);
        when(mockedUserInfo.getEmail())
            .thenReturn(idamUserEmail);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            idamUserId,
            idamUserEmail
        )).thenReturn(gpOn);
    }


    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId,
                                     TaskRoleResource taskRoleResource, String userId) {
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
        taskResource.setAssignee(userId);
        taskResource.setState(ASSIGNED);

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType,
                                     String taskId, TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            ASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("unclaimCaseId1");
        taskResource.setAssignee(IDAM_USER_ID);
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

}

