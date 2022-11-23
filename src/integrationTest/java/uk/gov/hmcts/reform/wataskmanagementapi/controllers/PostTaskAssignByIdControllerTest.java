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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.AssignTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SECONDARY_IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.THIRD_IDAM_USER_ID;

@SuppressWarnings("checkstyle:LineLength")
class PostTaskAssignByIdControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/assign";
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
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private UserInfo mockedSecondaryUserInfo;
    @MockBean
    private IdamService idamService;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    private ServiceMocks mockServices;
    private String taskId;

    private RoleAssignmentResource assignerRoleAssignmentResource;
    private RoleAssignmentResource assigneeRoleAssignmentResource;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

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
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    void should_return_500_with_application_problem_response_when_assign_call_fails(
        String jurisdiction, String caseType) throws Exception {

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
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        //assignee permissions : own, execute
        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);
        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(new AssignTaskRequest(SECONDARY_IDAM_USER_ID)))
        ).andExpect(
            ResultMatcher.matchAll(
                status().is5xxServerError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-assign-error"),
                jsonPath("$.title").value("Task Assign Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value(
                    "Task Assign Error: Task assign partially succeeded. "
                    + "The Task state was updated to assigned, but the Task could not be assigned.")
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    void should_return_500_with_application_problem_response_when_assign_call_fails_due_to_missing_assignee_id(
        String jurisdiction, String caseType) throws Exception {

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
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        //assignee permissions : own, execute
        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(false);

        doThrow(FeignException.FeignServerException.class).when(camundaServiceApi).assignTask(any(), any(), any());

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(new AssignTaskRequest(null)))
        ).andExpectAll(
            status().is5xxServerError(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$.message").value("IdamUserId cannot be null"),
            jsonPath("$.status").value(500)
        );
    }

    @Test
    public void should_return_a_401_when_the_user_did_not_have_any_roles() throws Exception {
        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);
        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(new AssignTaskRequest(SECONDARY_IDAM_USER_ID)))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_JSON_VALUE),
            jsonPath("$.error").value("Unauthorized"),
            jsonPath("$.status").value(401),
            jsonPath("$.message").value(
                "User did not have sufficient permissions to perform this action"));
    }

    @Test
    public void should_return_a_404_if_task_does_not_exist() throws Exception {
        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            "SCSS", "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        CompleteTaskRequest request = new CompleteTaskRequest(new CompletionOptions(true));
        String nonExistentTaskId = "00000000-0000-0000-0000-000000000000";

        mockMvc.perform(
            post("/task/" + nonExistentTaskId + "/assign")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(request))
                .content(asJsonString(new AssignTaskRequest(SECONDARY_IDAM_USER_ID)))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
            jsonPath("$.title").value("Task Not Found Error"),
            jsonPath("$.status").value(404),
            jsonPath("$.detail").value(
                "Task Not Found Error: The task could not be found.")
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void should_return_a_403_when_the_assigner_does_not_have_manage_permission(
        String jurisdiction, String caseType) throws Exception {

        //Same user has been used as a assigner and assignee
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker", true, true, false, false, false,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            jurisdiction, "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(roleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(roleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                + "The user assigning the Task has failed the Role Assignment checks performed.")
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void should_return_a_403_when_the_assignee_does_not_have_execute_or_own_permissions(
        String jurisdiction, String caseType) throws Exception {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, true, false,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);


        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            jurisdiction, "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(roleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(roleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                + "The user being assigned the Task has failed the Role Assignment checks performed.")
        );
    }


    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void assigner_should_assign_a_task_to_assignee_with_grant_type_challenged(
        String jurisdiction, String caseType) throws Exception {

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
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        //assignee permissions : own, execute
        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);
        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);


        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void assigner_should_not_assign_a_task_to_assignee_with_grant_type_challenged_and_excluded(
        String jurisdiction, String caseType) throws Exception {

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
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        //assignee permissions : own, execute
        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_JUDICIARY_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        //Excluded role
        assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_JUDICIAL)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                + "The user being assigned the Task has failed the Role Assignment checks performed.")
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void assigner_should_assign_a_task_to_assignee_with_grant_type_specific(
        String jurisdiction, String caseType) throws Exception {

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
        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);


        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum",
        "WA, WaCaseType"
    })
    public void assigner_should_assign_a_task_to_assignee_with_grant_type_specific_and_excluded(
        String jurisdiction, String caseType) throws Exception {

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

        //assignee excluded role
        assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN.getRoleName(),
            true, true, true, true, true, true,
            new String[]{}, 1, false,
            TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.EXCLUDED_CHALLENGED_ACCESS_ADMIN_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);
        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(mockedUserInfo.getUid()), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(HttpStatus.NO_CONTENT.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum, true, NO_CONTENT",
        "WA, WaCaseType, true, NO_CONTENT",
        "WA, WaCaseType, false, FORBIDDEN"
    })
    public void assigner_should_assign_a_task_to_assignee_with_valid_granular_permission(
        String jurisdiction, String caseType, boolean assign, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            false,false,assign,false,false,
            false,false
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
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(IDAM_USER_ID), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        //assignee permissions : own, execute
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA, Asylum, true, NO_CONTENT",
        "WA, WaCaseType, true, NO_CONTENT",
        "WA, WaCaseType, false, FORBIDDEN"
    })
    public void assigner_should_assign_a_task_to_themselves_with_valid_granular_permission(
        String jurisdiction, String caseType, boolean claim, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            claim,false,false,false,false,
            false,false
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assignerTaskRoleResource);

        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, assigneeTaskRoleResource);

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

        //assignee permissions : own, execute
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);

        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(IDAM_USER_ID), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false, false, false, true, false, NO_CONTENT",
        "true, false, true, false, false, NO_CONTENT",
        "false, false, false, false, true, NO_CONTENT",
        "false, true, true, false, false, NO_CONTENT",
        "false, false, false, false, false, FORBIDDEN",
    })
    public void assigner_should_assign_a_already_assigned_task_to_someone_to_themselves_with_valid_granular_permission(
        boolean claim, boolean assign, boolean unassign, boolean unassignClaim, boolean unassignAssign, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            claim,false, assign, unassign,false,
            unassignClaim, unassignAssign
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assignerTaskRoleResource, SECONDARY_IDAM_USER_ID);

        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assigneeTaskRoleResource, SECONDARY_IDAM_USER_ID);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);

        //assignee permissions : own, execute
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);

        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(IDAM_USER_ID), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false, false, true, NO_CONTENT",
        "true, true, false, NO_CONTENT",
        "false, false, false, FORBIDDEN",
    })
    public void assigner_should_assign_a_already_assigned_task_to_someone_to_someone_else_with_valid_granular_permission(
        boolean assign, boolean unassign, boolean unassignAssign, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            false,false, assign, unassign,false,
            false, unassignAssign
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assignerTaskRoleResource, THIRD_IDAM_USER_ID);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(IDAM_USER_ID), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);


        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assigneeTaskRoleResource, THIRD_IDAM_USER_ID);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        //assignee permissions : own, execute
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);


        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false, false, true, NO_CONTENT",
        "true, true, false, NO_CONTENT",
        "false, false, false, FORBIDDEN",
    })
    public void assigner_should_assign_a_already_assigned_task_to_themselves_to_someone_else_with_valid_granular_permission(
        boolean assign, boolean unclaim, boolean unclaimAssign, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            false,unclaim, assign, false,unclaimAssign,
            false, false
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assignerTaskRoleResource, IDAM_USER_ID);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            eq(IDAM_USER_ID), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);


        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assigneeTaskRoleResource, IDAM_USER_ID);

        List<RoleAssignment> assigneeRoles = new ArrayList<>();

        //assignee permissions : own, execute
        roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assigneeRoles, roleAssignmentRequest);

        assigneeRoleAssignmentResource = new RoleAssignmentResource(assigneeRoles);

        //Assignee
        lenient().when(roleAssignmentServiceApi.getRolesForUser(
            eq(SECONDARY_IDAM_USER_ID), any(), any()
        )).thenReturn(assigneeRoleAssignmentResource);


        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest(SECONDARY_IDAM_USER_ID);

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
        "false, true, NO_CONTENT",
        "true, false, NO_CONTENT",
        "false, false, FORBIDDEN",
    })
    public void assigner_should_unassign_a_already_assigned_task_with_valid_granular_permission(
        boolean unassign, boolean unclaim, HttpStatus status) throws Exception {

        //assigner permission : assign
        TaskRoleResource assignerTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleName(),
            false, false, false, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE.getRoleCategory().name(),
            null, null,false,false,false,
            false, unclaim, false, unassign, false,
            false, false
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assignerTaskRoleResource, SECONDARY_IDAM_USER_ID);

        List<RoleAssignment> assignerRoles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_HEARING_PANEL_JUDGE)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(assignerRoles, roleAssignmentRequest);
        assignerRoleAssignmentResource = new RoleAssignmentResource(assignerRoles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(assignerRoleAssignmentResource);


        //standard role
        TaskRoleResource assigneeTaskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, assigneeTaskRoleResource, SECONDARY_IDAM_USER_ID);

        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        AssignTaskRequest assignTaskRequest = new AssignTaskRequest();

        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(assignTaskRequest))
        ).andExpectAll(
            status().is(status.value())
        );
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId,
                                     TaskRoleResource taskRoleResource) {
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource, null);
    }

    private void insertDummyTaskInDb(String jurisdiction, String caseType, String taskId,
                                     TaskRoleResource taskRoleResource, String assignee) {
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
        taskResource.setAssignee(assignee);
        if (assignee != null) {
            taskResource.setState(CFTTaskState.ASSIGNED);
        }

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

}

