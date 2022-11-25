package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ExecutionTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.BusinessContext;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.ExecutionType;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.TaskSystem;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestMap;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.options.CompletionOptions;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.InitiateTaskOperation.INITIATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_ASSIGNEE;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskAttributeDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class GetTaskByIdControllerTest extends SpringBootIntegrationBaseTest {

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
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private UserInfo mockedUserInfo;
    @MockBean
    private IdamService idamService;
    @Mock
    CcdDataServiceApi ccdDataServiceApi;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private CaseDetails caseDetails;



    private String taskId;
    private ServiceMocks mockServices;


    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();

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
    public void should_return_a_200_when_get_by_standard_tribunal_case_worker() throws Exception {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, false, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource);

        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, roleAssignmentRequest);

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.OK.value()),
            jsonPath("$.task.id").value(taskId)
        );
    }

    @Test
    public void should_return_a_200_when_get_by_standard_judge() throws Exception {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_JUDGE_PUBLIC.getRoleName(),
            true, true, false, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_JUDGE_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource);

        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_JUDGE_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, roleAssignmentRequest);
        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.OK.value()),
            jsonPath("$.task.id").value(taskId)
        );
    }

    @Test
    public void should_return_a_200_when_get_by_challenge_access_admin() throws Exception {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN.getRoleName(),
            true, true, false, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource);

        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.CHALLENGED_ACCESS_ADMIN)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, roleAssignmentRequest);

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.OK.value()),
            jsonPath("$.task.id").value(taskId)
        );
    }

    @Test
    void should_return_a_404_when_id_is_not_found() throws Exception {

        mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        // Role attribute is IA
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            allTestRoles
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isNotFound(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
            jsonPath("$.title").value("Task Not Found Error"),
            jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()),
            jsonPath("$.detail").value("Task Not Found Error: The task could not be found.")
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
            get("/task/" + "taskId")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
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
            get("/task/" + nonExistentTaskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(asJsonString(request))
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

    @Test
    void should_return_a_403_when_restricted_role_is_given() throws Exception {

        createTaskAndRoleAssignments(UNASSIGNED, "getTaskCaseId1");
        mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        // Role attribute is IA
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            allTestRoles
        );

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isForbidden());

    }

    @Test
    public void should_return_a_403_when_the_user_jurisdiction_did_not_match() throws Exception {
        createTaskAndRoleAssignments(UNASSIGNED, "getTaskCaseId2");

        mockServices.mockUserInfo();

        // create role assignments Organisation and SCSS and Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            mockServices.createRoleAssignmentsWithJurisdiction("SCSS", "getTaskCaseId2")
        );
        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                + "The request failed the Role Assignment checks performed.")
        );

    }

    @Test
    public void should_return_a_403_when_the_user_region_did_not_match() throws Exception {
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, false, true, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, taskRoleResource);

        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .region("1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, roleAssignmentRequest);

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                    + "The request failed the Role Assignment checks performed.")
        );

    }


    @Test
    @Execution(ExecutionMode.CONCURRENT)
    void should_return_404_when_initiation_request_failed_to_retrieve_data_from_ccd() throws Exception {

        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

        // Role attribute is IA
        List<RoleAssignment> allTestRoles = new ArrayList<>();
        roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
            .forEach(roleType -> {
                RoleAssignment roleAssignment = mockServices.createBaseAssignment(
                    UUID.randomUUID().toString(), "tribunal-caseworker",
                    roleType,
                    Classification.PUBLIC,
                    roleAttributes
                );
                allTestRoles.add(roleAssignment);
            }));

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            allTestRoles
        );

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("getTaskCaseId3")))
            .thenThrow(new RuntimeException("some error"));

        ZonedDateTime createdDate = ZonedDateTime.now();
        ZonedDateTime dueDate = createdDate.plusDays(1);
        String formattedDueDate = CamundaTime.CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = new HashMap<>(Map.of(
            TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
            TASK_CASE_ID.value(), "getTaskCaseId3",
            DUE_DATE.value(), formattedDueDate,
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_ASSIGNEE.value(), "someAssignee"
        ));

        InitiateTaskRequestMap initiateTaskRequest = new InitiateTaskRequestMap(INITIATION, taskAttributes);

        //first initiate call
        mockMvc
            .perform(post("/task/" + taskId + "/initiation")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(initiateTaskRequest)))
            //.andDo(print())
            .andExpectAll(
                status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/generic-server-error"),
                jsonPath("$.title").value("Generic Server Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value("Generic Server Error: The action could not be "
                                           + "completed because there was a problem when initiating the task.")
            );

        //second initiate call
        mockMvc
            .perform(post("/task/" + taskId + "/initiation")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(initiateTaskRequest)))
            //.andDo(print())
            .andExpectAll(
                status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/generic-server-error"),
                jsonPath("$.title").value("Generic Server Error"),
                jsonPath("$.status").value(500),
                jsonPath("$.detail").value("Generic Server Error: The action could not be "
                                           + "completed because there was a problem when initiating the task.")
            );

        //retrieve task
        mockMvc.perform(
                get("/task/" + taskId)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(print())
            .andExpectAll(
                status().is(HttpStatus.NOT_FOUND.value()),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/task-not-found-error"),
                jsonPath("$.title").value("Task Not Found Error"),
                jsonPath("$.status").value(HttpStatus.NOT_FOUND.value()),
                jsonPath("$.detail").value("Task Not Found Error: The task could not be found.")
            );

    }

    @Test
    public void should_return_different_permissions_when_given_case_role_assignment() throws Exception {
        String caseId = UUID.randomUUID().toString();

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, false, true, false, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );

        TaskRoleResource taskRoleResourceCase = new TaskRoleResource(
            TestRolesWithGrantType.SPECIFIC_CASE_MANAGER.getRoleName(),
            true, true, true, false, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.SPECIFIC_CASE_MANAGER.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId, Set.of(taskRoleResource, taskRoleResourceCase), "caseId1");

        String taskId2 = UUID.randomUUID().toString();
        TaskRoleResource taskRoleResource2 = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, false, true, false, true, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb("WA", "WaCaseType", taskId2, taskRoleResource);

        List<RoleAssignment> roles = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, roleAssignmentRequest);

        RoleAssignmentRequest caseRoleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.SPECIFIC_CASE_MANAGER)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("WA")
                    .caseType("WaCaseType")
                    .caseId("caseId1")
                    .build()
            )
            .build();

        createRoleAssignment(roles, caseRoleAssignmentRequest);

        RoleAssignmentResource roleAssignmentResource = new RoleAssignmentResource(roles);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        //Assigner
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(roleAssignmentResource);

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.OK.value()),
            jsonPath("$.task.id").value(taskId),
            jsonPath("$.task.permissions.values", hasSize(4)),
            jsonPath("$.task.permissions.values[0]").value("Read"),
            jsonPath("$.task.permissions.values[1]").value("Own"),
            jsonPath("$.task.permissions.values[2]").value("Execute"),
            jsonPath("$.task.permissions.values[3]").value("Cancel")
        );

        mockMvc.perform(
            get("/task/" + taskId2)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is(HttpStatus.OK.value()),
            jsonPath("$.task.id").value(taskId2),
            jsonPath("$.task.permissions.values", hasSize(3)),
            jsonPath("$.task.permissions.values[0]").value("Read"),
            jsonPath("$.task.permissions.values[1]").value("Execute"),
            jsonPath("$.task.permissions.values[2]").value("Cancel")
        );
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

    private void insertDummyTaskInDb(String jurisdiction,
                                     String caseType,
                                     String taskId,
                                     TaskRoleResource taskRoleResource) {
        insertDummyTaskInDb(jurisdiction, caseType, taskId, Set.of(taskRoleResource), "caseId1");
    }

    private void insertDummyTaskInDb(String jurisdiction,
                                     String caseType,
                                     String taskId,
                                     Set<TaskRoleResource> taskRoleResources, String caseId) {
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
        taskResource.setCaseId(caseId);

        taskRoleResources.forEach(t -> t.setTaskId(taskId));
        Set<TaskRoleResource> taskRoleResourceSet = taskRoleResources;
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

    private TaskResource createTask(String taskId, Set<TaskRoleResource> taskRoleResources,
                                    String caseId) {
        return new TaskResource(
            taskId,
            "aTaskName",
            "startAppeal",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            CFTTaskState.ASSIGNED,
            TaskSystem.SELF,
            SecurityClassification.PUBLIC,
            "title",
            "a description",
            null,
            0,
            0,
            "someAssignee",
            false,
            new ExecutionTypeResource(ExecutionType.MANUAL, "Manual", "Manual Description"),
            new WorkTypeResource("routine_work", "Routine work"),
            "JUDICIAL",
            false,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"),
            caseId,
            "WaCaseType",
            "TestCase",
            "WA",
            "1",
            "TestRegion",
            "765324",
            "Taylor House",
            BusinessContext.CFT_TASK,
            "Some termination reason",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            taskRoleResources,
            "caseCategory",
            ADDITIONAL_PROPERTIES,
            "nextHearingId",
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2021-05-09T20:15:45.345875+01:00")
        );
    }

}

