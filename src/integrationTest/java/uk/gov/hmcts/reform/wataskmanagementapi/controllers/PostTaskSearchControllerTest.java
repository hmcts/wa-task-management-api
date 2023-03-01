package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequestMapper;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortField;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortOrder;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SortingParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.SearchOperator.IN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.search.parameter.SearchParameterKey.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN_FOR_EXCEPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostTaskSearchControllerTest extends SpringBootIntegrationBaseTest {

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
    @SpyBean
    private CftQueryService cftQueryService;
    @Mock
    private UserInfo mockedUserInfo;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
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

    @ParameterizedTest
    @ValueSource(strings = {
        "/task", "/task?first_result=0", "/task?max_results=1", "/task?first_result=0&max_results=1"
    })
    void should_return_a_200_when_restricted_role_is_given(String uri) throws Exception {

        String caseId = "searchCriteriaCaseId1";
        mockServices.mockUserInfo();

        // Role attribute is IA
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId(caseId)
                    .build()
            )
            .build();
        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(caseId, taskId, "IA", "Asylum", taskRoleResource);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SCSS"))
        ));

        mockMvc.perform(
            post(uri)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("total_records").value(0),
                jsonPath("$.tasks").isEmpty()
            )
        );
    }

    /*
        Single Task is created with two role assignments one with IA and Organisation and
        other with SSCS and Case.
        When a task is searched with SSCS , test returns only single result with SSCS Jurisdiction
     */
    @Test
    void should_return_single_task_when_two_role_assignments_with_one_restricted_is_given() throws Exception {
        String caseId = "searchCriteriaCaseId2";
        mockServices.mockUserInfo();
        // create role assignments with IA, Organisation and SCSS , Case
        List<RoleAssignment> roleAssignments = mockServices.createRoleAssignmentsWithSCSSandIA(caseId);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(caseId, taskId, "SSCS", "Asylum", taskRoleResource);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SSCS"))
        ));

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("total_records").value(1),
            jsonPath("$.tasks").isNotEmpty(),
            jsonPath("$.tasks.length()").value(1),
            jsonPath("$.tasks[0].jurisdiction").value("SSCS")
        );
    }

    @Test
    void should_return_a_200_with_search_results_and_warnings() throws Exception {
        String caseId = "searchCriteriaCaseId3";
        mockServices.mockUserInfo();

        // Role attribute is IA
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId(caseId)
                    .build()
            )
            .build();
        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );

        String roleAssignmentId = UUID.randomUUID().toString();

        insertDummyTaskWithWarningsAndAdditionalPropertiesInDb(caseId,
                                                               taskId,
                                                               "IA",
                                                               "Asylum",
                                                               roleAssignmentId,
                                                               taskRoleResource);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
        ));

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("total_records").value(1),
            jsonPath("$.tasks").isNotEmpty(),
            jsonPath("$.tasks.length()").value(1),
            jsonPath("tasks[0].warning_list.values.size()").value(2),
            jsonPath("tasks[0].warning_list.values[0].warningCode").value("Code1"),
            jsonPath("tasks[0].warning_list.values[0].warningText").value("Text1"),
            jsonPath("tasks[0].warning_list.values[1].warningCode").value("Code2"),
            jsonPath("tasks[0].warning_list.values[1].warningText").value("Text2"),
            jsonPath("tasks[0].additional_properties['roleAssignmentId']").value(roleAssignmentId)
        );
    }

    @Test
    void should_return_a_200_with_limited_tasks_with_pagination() throws Exception {
        String caseId = "searchCriteriaCaseId4";
        mockServices.mockUserInfo();

        // Role attribute is IA
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId(caseId)
                    .build()
            )
            .build();
        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );

        insertDummyTaskInDb(caseId, UUID.randomUUID().toString(),"IA","Asylum", taskRoleResource);
        insertDummyTaskInDb(caseId, UUID.randomUUID().toString(),"IA","Asylum", taskRoleResource);
        insertDummyTaskInDb(caseId, UUID.randomUUID().toString(),"IA","Asylum", taskRoleResource);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
        ));

        mockMvc.perform(
            post("/task?first_result=0&max_results=2")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("total_records").value(3),
            jsonPath("$.tasks").isNotEmpty(),
            jsonPath("$.tasks.length()").value(2)
        );
    }

    @Test
    void should_return_task_with_old_permissions_when_granular_permission_flag_off() throws Exception {
        String caseId = "searchCriteriaCaseId3";
        mockServices.mockUserInfo();
        // create role assignments with IA, Organisation and SCSS , Case
        List<RoleAssignment> roleAssignments = mockServices.createRoleAssignmentsWithSCSSandIA(caseId);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
            taskId, OffsetDateTime.now(), true, true, true, true,
            true, true, true, true, true, true
        );

        insertDummyTaskInDb(caseId, taskId, "SSCS", "Asylum", taskRoleResource);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SSCS"))
        ));

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("total_records").value(1),
            jsonPath("$.tasks").isNotEmpty(),
            jsonPath("$.tasks.length()").value(1),
            jsonPath("$.tasks[0].jurisdiction").value("SSCS"),
            jsonPath("$.tasks[0].permissions.values[0]").value("Read"),
            jsonPath("$.tasks[0].permissions.values[1]").value("Own"),
            jsonPath("$.tasks[0].permissions.values[2]").value("Execute"),
            jsonPath("$.tasks[0].permissions.values.length()").value(3)
        ).andReturn();

    }

    @Test
    void should_return_task_with_granular_permissions_when_permission_flag_on() throws Exception {
        String caseId = "searchCriteriaCaseId4";
        mockServices.mockUserInfo();
        // create role assignments with IA, Organisation and SCSS , Case
        List<RoleAssignment> roleAssignments = mockServices.createRoleAssignmentsWithSCSSandIA(caseId);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
            taskId, OffsetDateTime.now(), true, true, true, true,
            true, true, true, true, true, false
        );

        insertDummyTaskInDb(caseId, taskId, "SSCS", "Asylum", taskRoleResource);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SSCS"))
        ));

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_4_GRANULAR_PERMISSION_RESPONSE,
            mockedUserInfo.getUid(),
            mockedUserInfo.getEmail()
        )).thenReturn(true);

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchTaskRequest))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andDo(MockMvcResultHandlers.print())
            .andExpectAll(
                status().isOk(),
                jsonPath("total_records").value(1),
                jsonPath("$.tasks").isNotEmpty(),
                jsonPath("$.tasks.length()").value(1),
                jsonPath("$.tasks[0].jurisdiction").value("SSCS"),
                jsonPath("$.tasks[0].permissions.values[0]").value("Read"),
                jsonPath("$.tasks[0].permissions.values[1]").value("Own"),
                jsonPath("$.tasks[0].permissions.values[2]").value("Execute"),
                jsonPath("$.tasks[0].permissions.values[3]").value("Complete"),
                jsonPath("$.tasks[0].permissions.values[4]").value("CompleteOwn"),
                jsonPath("$.tasks[0].permissions.values[5]").value("CancelOwn"),
                jsonPath("$.tasks[0].permissions.values[6]").value("Claim"),
                jsonPath("$.tasks[0].permissions.values[7]").value("Unclaim"),
                jsonPath("$.tasks[0].permissions.values[8]").value("Assign"),
                jsonPath("$.tasks[0].permissions.values[9]").value("Unassign"),
                jsonPath("$.tasks[0].permissions.values[10]").value("UnclaimAssign"),
                jsonPath("$.tasks[0].permissions.values[11]").value("UnassignClaim"),
                jsonPath("$.tasks[0].permissions.values.length()").value(12)
            ).andReturn();
    }

    @Test
    void should_return_200_empty_list_when_work_types_did_not_match() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA")),
            new SearchParameterList(WORK_TYPE, IN, singletonList("access_requests"))
        ));

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchTaskRequest))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isOk(),
                    jsonPath("total_records").value(0),
                    jsonPath("$.tasks").isEmpty()
                ));
    }

    @Test
    void should_return_400_bad_request_when_work_types_did_not_exist() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal,review_case");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA")),
            new SearchParameterList(WORK_TYPE, IN, singletonList("invalid_value"))
        ));

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchTaskRequest))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations").isNotEmpty(),
                    jsonPath("$.violations.[0].field").value("invalid_value"),
                    jsonPath("$.violations.[0].message")
                        .value("work_type must be one of [hearing_work, upper_tribunal, routine_work, "
                               + "decision_making_work, applications, priority, access_requests, "
                               + "error_management, review_case, evidence, follow_up]")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_input_request() throws Exception {

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                jsonPath("$.title").value("Bad Request"),
                jsonPath("$.status").value(400),
                jsonPath("$.detail")
                    .value("Unexpected end-of-input: expected close marker for Object "
                           + "(start marker at [Source: (org.springframework."
                           + "util.StreamUtils$NonClosingInputStream); line: 1, column: 1])")
            );
    }

    @Test
    void should_return_400_bad_request_when_invalid_body_request() throws Exception {
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{this is invalid}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Unexpected character ('t' (code 116)): was expecting "
                               + "double-quote to start field name")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_search_parameter_key() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "someInvalidKey",
                    "operator": "IN",
                    "values": [
                      "aValue"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0].key")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_camelCase_work_type_search_parameter_key() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "workType",
                    "operator": "IN",
                    "values": [
                      "aValue"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0].key")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_search_parameter_operator() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "work_type",
                    "operator": "INVALID",
                    "values": [
                      "aValue"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value(
                        "Invalid request field: search_parameters.[0]: Each search_parameter element "
                        + "must have 'key', 'values' and 'operator' fields present and populated.")
                ));

    }

    @Test
    void should_return_400_bad_request_when_no_search_parameters_provided() throws Exception {
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(new SearchTaskRequest(emptyList())))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations").isNotEmpty(),
                    jsonPath("$.violations.[0].field").value("search_parameters"),
                    jsonPath("$.violations.[0].message")
                        .value("At least one search_parameter element is required.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_no_operator_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "values": [
                      "ia", "sscs"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value(
                        "Invalid request field: search_parameters.[0]: Each search_parameter element "
                        + "must have 'key', 'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_empty_string_value_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "",
                    "values": [
                      "ia", "sscs"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value(
                        "Invalid request field: search_parameters.[0]: Each search_parameter element "
                        + "must have 'key', 'values' and 'operator' fields present and populated.")
                ));

    }

    @Test
    void should_return_400_bad_request_when_null_operator_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": null,
                    "values": [
                      "ia", "sscs"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value(
                        "Invalid request field: search_parameters.[0]: Each search_parameter element "
                        + "must have 'key', 'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_operator_with_null_value_provided() throws Exception {
        String content = """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "null",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andDo(MockMvcResultHandlers.print())
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                        ),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0]: Each search_parameter "
                               + "element must have 'key', "
                               + "'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_null_key_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": null,
                    "operator": "IN",
                    "values": [
                      "ia", "something"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations").isNotEmpty(),
                    jsonPath("$.violations.[0].field").value("search_parameters[0].key"),
                    jsonPath("$.violations.[0].message")
                        .value("Each search_parameter element must have 'key', 'values' "
                               + "and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_key_and_values_are_empty() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "",
                    "operator": "IN",
                    "values": [
                      "", ""
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                        ),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0].key")
                ));
    }

    @Test
    void should_return_400_bad_request_when_empty_string_key_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "",
                    "operator": "IN",
                    "values": [
                      "ia", "something"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                        ),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0].key")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_first_result_pagination_param_provided() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "ia"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task?first_result=-1&max_results=2")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations.[0].field")
                        .value("search_with_criteria.first_result"),
                    jsonPath("$.violations.[0].message")
                        .value("first_result must not be less than zero")
                ));
    }

    @Test
    void should_return_a_400_with_empty_search_results_with_negative_maxResults_pagination() throws Exception {
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "ia"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task?first_result=0&max_results=-2")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations.[0].field")
                        .value("search_with_criteria.max_results"),
                    jsonPath("$.violations.[0].message")
                        .value("max_results must not be less than one")
                ));
    }

    @Test
    void should_return_200_and_accept_Request_when_theres_at_least_one_value() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "ia", null
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isOk()));
    }

    @Test
    void should_return_200_and_accept_Request_when_values_are_null() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      null
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isOk()));
    }

    @Test
    void should_return_200_and_accept_work_type_values() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        String content = """
            {
                "search_parameters": [
                  {
                    "key": "work_type",
                    "operator": "IN",
                    "values": [
                      "hearing_work","upper_tribunal","routine_work","routine_work","decision_making_work",
                      "applications","priority","error_management","access_requests","review_case","evidence",
                      "follow_up"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(status().isOk());
    }

    @Test
    void should_return_400_bad_request_when_invalid_case_role_category_search_parameter_key()
        throws Exception {

        String content = """
            {
                "search_parameters": [
                  {
                    "key": "roleCategory",
                    "operator": "IN",
                    "values": [
                      "aValue"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value("Invalid request field: search_parameters.[0].key")
                ));
    }

    @Test
    void should_return_400_bad_request_when_invalid_case_available_tasks_search_parameter_key()
        throws Exception {

        String content = """
            {
                "search_parameters": [
                "request_context": "INVALID_VALUE",
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "ia"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail").value("Invalid request field: search_parameters.[0]: "
                                               + "Each search_parameter element must have "
                                               + "'key', 'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_200_correctly_parse_is_available_task_only_true()
        throws Exception {
        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));
        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        String content = """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(content)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(expectedReq);

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            searchRequest,
            accessControlResponse,
            false,
            false
        );
    }

    @Test
    void should_return_200_correctly_parse_is_available_task_only_false() throws Exception {
        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        String content = """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );
        SearchRequest searchRequest = SearchTaskRequestMapper.map(expectedReq);

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            searchRequest,
            accessControlResponse,
            false,
            false
        );
    }

    @Test
    void should_return_200_given_sort_by_parameter_should_support_camelCase() throws Exception {
        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        String content = """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "ia"
                    ]
                  }
                ],
                "sorting_parameters": [
                  {
                    "sort_by": "dueDate",
                    "sort_order": "asc"
                  }
                ]
              }
            """;

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, IN, asList("ia"))
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_CAMEL_CASE, SortOrder.ASCENDANT))
        );
        SearchRequest searchRequest = SearchTaskRequestMapper.map(expectedReq);

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            searchRequest,
            accessControlResponse,
            false,
            false
        );
    }

    @Test
    void should_return_200_given_sort_by_parameter_should_support_snake_case() throws Exception {
        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        String content = """
            {
                "request_context": "AVAILABLE_TASKS",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ],
                "sorting_parameters": [
                  {
                    "sort_by": "due_date",
                    "sort_order": "asc"
                  }
                ]
              }
            """;
        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            RequestContext.AVAILABLE_TASKS,
            List.of(
                new SearchParameterList(JURISDICTION, IN, List.of("IA"))
            ),
            singletonList(new SortingParameter(SortField.DUE_DATE_SNAKE_CASE, SortOrder.ASCENDANT))
        );
        SearchRequest searchRequest = SearchTaskRequestMapper.map(expectedReq);

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            searchRequest,
            accessControlResponse,
            false,
            false
        );
    }

    @ParameterizedTest
    @EnumSource(RequestContext.class)
    void should_correctly_parse_request_context_and_return_200(RequestContext context) throws Exception {
        UserInfo userInfo = mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        String content = String.format("""
            {
                "request_context": "%s",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """, context.toString());
        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            context,
            asList(
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );

        SearchRequest searchRequest = SearchTaskRequestMapper.map(expectedReq);

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            searchRequest,
            accessControlResponse,
            false,
            false
        );
    }

    @Test
    void should_return_a_400_for_invalid_request_context() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        String content = """
            {
                "request_context": "GENERAL_SEARCH",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value(
                        "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                    ),
                jsonPath("$.title").value("Bad Request"),
                jsonPath("$.status").value(400),
                jsonPath("$.detail").value("Invalid request field: request_context")
            ));
    }

    @Test
    void should_return_a_400_for_empty_request_context() throws Exception {
        final List<String> roleNames = singletonList("tribunal-caseworker");

        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        String content = """
            {
                "request_context": "",
                "search_parameters": [
                  {
                    "key": "jurisdiction",
                    "operator": "IN",
                    "values": [
                      "IA"
                    ]
                  }
                ]
              }
            """;
        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(content)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isBadRequest(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type")
                    .value(
                        "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                    ),
                jsonPath("$.title").value("Bad Request"),
                jsonPath("$.status").value(400),
                jsonPath("$.detail").value("Invalid request field: request_context")
            ));
    }

    @Test
    void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() throws Exception {

        UserInfo userInfo = mockServices.mockUserInfo();

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(emptyList()));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(idamWebApi.userInfo(any())).thenReturn(userInfo);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA")),
            new SearchParameterList(WORK_TYPE, IN, singletonList("access_requests"))
        ));
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchTaskRequest))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0))
            .andExpect(jsonPath("total_records").value(0));

    }


    @DisplayName("Should return 502 when idam service is down")
    @Test
    void should_return_status_code_502_when_idam_service_is_down() throws Exception {

        mockServices.throwFeignExceptionForIdam();

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
        ));

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN_FOR_EXCEPTION)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(asJsonString(searchTaskRequest))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
                status().isBadGateway(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value(
                    "https://github.com/hmcts/wa-task-management-api/problem/downstream-dependency-error"),
                jsonPath("$.title").value("Downstream Dependency Error"),
                jsonPath("$.status").value(502),
                jsonPath("$.detail").value(
                    "Downstream dependency did not respond as expected "
                    + "and the request could not be completed.")
            );
    }


    private void insertDummyTaskInDb(String caseId, String taskId, String jurisdiction, String caseType,
                                     TaskRoleResource taskRoleResource) {
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

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void insertDummyTaskWithWarningsAndAdditionalPropertiesInDb(String caseId, String taskId,
                                                                        String jurisdiction, String caseType,
                                                                        String roleAssignmentId,
                                                                        TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "aTaskName",
            "reviewTheAppeal",
            ASSIGNED
        );

        List<NoteResource> warnings = List.of(
            new NoteResource("Code1","WARNING",null,"Text1"),
            new NoteResource("Code2","WARNING",null,"Text2")
        );

        taskResource.setDescription("aDescription");
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseType);
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);
        taskResource.setAssignee(IDAM_USER_ID);
        taskResource.setWorkTypeResource(new WorkTypeResource("decision_making_work", "Decision Making work"));
        taskResource.setNotes(warnings);
        taskResource.setHasWarnings(true);
        taskResource.setAdditionalProperties(Map.of("roleAssignmentId", roleAssignmentId));
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);

    }
}

