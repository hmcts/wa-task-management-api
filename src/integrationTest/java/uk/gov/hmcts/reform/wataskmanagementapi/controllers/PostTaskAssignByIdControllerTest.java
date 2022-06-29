package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
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
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @MockBean
    private AccessControlService accessControlService;
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
        "IA",
        "WA"
    })
    void should_return_500_with_application_problem_response_when_task_update_call_fails(
        String jurisdiction) throws Exception {

        when(permissionEvaluatorService.hasAccess(any(), any(), any()))
            .thenReturn(true);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(false);

        CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);
        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            jurisdiction, "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(accessControlService.getRolesGivenUserId(SECONDARY_IDAM_USER_ID, IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        doThrow(FeignException.FeignServerException.class)
            .when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

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
                    "Task Assign Error: Task assign failed. Unable to update task state to assigned.")
            ));
    }

    @ParameterizedTest
    @CsvSource(value = {
        "IA",
        "WA"
    })
    void should_return_500_with_application_problem_response_when_assign_call_fails(
        String jurisdiction
    ) throws Exception {

        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            jurisdiction, "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(accessControlService.getRolesGivenUserId(SECONDARY_IDAM_USER_ID, IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(permissionEvaluatorService.hasAccess(any(), any(), any()))
            .thenReturn(true);

        CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

        doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());
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
    public void should_return_a_403_when_the_assigner_does_not_have_manage_permission(
        String jurisdiction, String caseType) throws Exception {

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            "tribunal-caseworker", true, true, false, false, false,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        insertDummyTaskInDb(jurisdiction, caseType, taskId, taskRoleResource);

        List<RoleAssignment> roleAssignmentsWithJurisdiction = mockServices.createRoleAssignmentsWithJurisdiction(
            jurisdiction, "caseId1");
        // create role assignments Organisation and SCSS , Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(accessControlService.getRolesGivenUserId(SECONDARY_IDAM_USER_ID, IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
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
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignmentsWithJurisdiction
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(accessControlService.getRolesGivenUserId(SECONDARY_IDAM_USER_ID, IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo,
                roleAssignmentsWithJurisdiction));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
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
    public void assigner_should_assign_a_task_to_assignee_with_grant_type_standard(
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
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
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
    public void assigner_should_not_assign_a_task_to_assignee_with_grant_type_standard_and_excluded(
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
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
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
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
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
        assignerAccessControlResponse = new AccessControlResponse(mockedUserInfo, assignerRoles);
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

