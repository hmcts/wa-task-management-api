package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.RequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterBoolean;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterList;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterRequestContext;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_TASK_QUERY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.BOOLEAN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.CONTEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator.IN;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.AVAILABLE_TASKS_ONLY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.REQUEST_CONTEXT;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.parameter.SearchParameterKey.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN_FOR_EXCEPTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SuppressWarnings("checkstyle:LineLength")
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
    @SpyBean
    private CftQueryService cftQueryService;

    private ServiceMocks mockServices;
    private String taskId;

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
        taskId = UUID.randomUUID().toString();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/task", "/task?first_result=0", "/task?max_results=1", "/task?first_result=0&max_results=1"
    })
    void should_return_a_200_when_restricted_role_is_given(String uri) throws Exception {

        mockServices.mockUserInfo();

        final List<String> roleNames = singletonList("tribunal-caseworker");

        // Role attribute is IA
        Map<String, String> roleAttributes = new HashMap<>();
        roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

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
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", "some-id")
        );
        when(camundaServiceApi.searchWithCriteriaAndPagination(
            any(), anyInt(), anyInt(), any())).thenReturn(camundaTasks);

        // Task created with Jurisdiction SSCS
        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SSCS"))
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

        mockServices.mockUserInfo();

        // create role assignments with IA, Organisation and SCSS , Case
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            mockServices.createRoleAssignmentsWithSCSSandIA()
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        List<CamundaTask> camundaTasks = List.of(
            mockServices.getCamundaTask("processInstanceId", taskId)
        );
        when(camundaServiceApi.searchWithCriteriaAndPagination(
            any(), anyInt(), anyInt(), any())).thenReturn(camundaTasks);

        when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

        // Task created with Jurisdiction SCSS
        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
            new SearchParameterList(JURISDICTION, IN, singletonList("SSCS"))
        ));

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(asJsonString(searchTaskRequest))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(
            ResultMatcher.matchAll(
                status().isOk(),
                jsonPath("total_records").value(1),
                jsonPath("$.tasks").isNotEmpty(),
                jsonPath("$.tasks.length()").value(1),
                jsonPath("$.tasks[0].jurisdiction").value("SSCS")
            ));
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

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            RELEASE_2_TASK_QUERY,
            accessControlResponse.getUserInfo().getUid(),
            IDAM_USER_EMAIL
        )).thenReturn(true);

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
        roleAttributes.put(RoleAttributeDefinition.WORK_TYPES.value(), "hearing_work,upper_tribunal");

        List<RoleAssignment> allTestRoles =
            mockServices.createTestRoleAssignmentsWithRoleAttributes(roleNames, roleAttributes);

        AccessControlResponse accessControlResponse = new AccessControlResponse(userInfo, allTestRoles);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(allTestRoles));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

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
                               + "error_management]")
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

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"someInvalidKey\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [\n"
                             + "        \"aValue\"\n"
                             + "      ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"workType\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [\n"
                             + "        \"aValue\"\n"
                             + "      ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"work_type\",\n"
                             + "      \"operator\": \"INVALID\",\n"
                             + "      \"values\": [\n"
                             + "        \"aValue\"\n"
                             + "      ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
                        "Invalid request field: search_parameters.[0]: Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"jurisdiction\",\n"
                             + "            \"values\": [\"ia\", \"sscs\"]\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
                        "Invalid request field: search_parameters.[0]: Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_empty_string_value_provided() throws Exception {
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"jurisdiction\",\n"
                             + "            \"values\": [\"ia\", \"sscs\"],\n"
                             + "            \"operator\": \"\"\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
                        "Invalid request field: search_parameters.[0]: Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.")
                ));

    }

    @Test
    void should_return_400_bad_request_when_null_operator_provided() throws Exception {
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"jurisdiction\",\n"
                             + "            \"values\": [\"ia\", \"sscs\"],\n"
                             + "            \"operator\": null\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
                        "Invalid request field: search_parameters.[0]: Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.")
                ));
    }

    @Test
    void should_return_400_bad_request_when_operator_with_null_value_provided() throws Exception {
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"jurisdiction\",\n"
                             + "            \"values\": [\"ia\", \"sscs\"],\n"
                             + "             \"operator\": \"null\"\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": null,\n"
                             + "            \"values\": [\"ia\", \"something\"],\n"
                             + "            \"operator\": \"IN\"\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"\",\n"
                             + "            \"values\": [\"\", \"\"],\n"
                             + "            \"operator\": \"IN\"\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "    \"search_parameters\": [\n"
                             + "        {\n"
                             + "            \"key\": \"\",\n"
                             + "            \"values\": [\"ia\", \"something\"],\n"
                             + "            \"operator\": \"IN\"\n"
                             + "        }\n"
                             + "    ]\n"
                             + "}")
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"jurisdiction\",\n"
                             + "      \"values\": [\"ia\", null],\n"
                             + "      \"operator\": \"IN\"\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"jurisdiction\",\n"
                             + "      \"values\": [null],\n"
                             + "      \"operator\": \"IN\"\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"work_type\",\n"
                             + "      \"values\": [\n"
                             + "        \"hearing_work\",\n"
                             + "        \"upper_tribunal\",\n"
                             + "        \"routine_work\",\n"
                             + "        \"decision_making_work\",\n"
                             + "        \"applications\",\n"
                             + "        \"priority\",\n"
                             + "        \"error_management\",\n"
                             + "        \"access_requests\"\n"
                             + "      ],\n"
                             + "      \"operator\": \"IN\"\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                ResultMatcher.matchAll(
                    status().isOk()));
    }

    @Test
    void should_return_400_bad_request_when_invalid_case_role_category_search_parameter_key()
        throws Exception {


        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"roleCategory\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [\n"
                             + "        \"aValue\"\n"
                             + "      ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
    void should_return_400_bad_request_when_invalid_case_available_tasks_only_search_parameter_key()
        throws Exception {

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"availableTtasksOnly\",\n"
                             + "      \"operator\": \"BOOLEAN\",\n"
                             + "      \"value\": true\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
    void should_return_400_bad_request_when_available_tasks_only_is_set_with_empty_value()
        throws Exception {

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"available_tasks_only\",\n"
                             + "      \"operator\": \"BOOLEAN\",\n"
                             + "      \"value\": \n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
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
                                               + "Unexpected character ('}' (code 125)): expected a value")
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.GRANULAR_PERMISSION_FEATURE,
            accessControlResponse.getUserInfo().getUid(),
            IDAM_USER_EMAIL
        )).thenReturn(false);

        mockMvc.perform(
                post("/task")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"available_tasks_only\",\n"
                             + "      \"operator\": \"BOOLEAN\",\n"
                             + "      \"value\": true\n"
                             + "    },\n"
                             + "    {\n"
                             + "      \"key\": \"jurisdiction\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [ \"IA\" ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            asList(
                new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, BOOLEAN, true),
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            expectedReq,
            accessControlResponse
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                RELEASE_2_TASK_QUERY,
                accessControlResponse.getUserInfo().getUid(),
                IDAM_USER_EMAIL
            )
        ).thenReturn(true);

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content("{\n"
                         + "  \"search_parameters\": [\n"
                         + "    {\n"
                         + "      \"key\": \"available_tasks_only\",\n"
                         + "      \"operator\": \"BOOLEAN\",\n"
                         + "      \"value\": false\n"
                         + "    },\n"
                         + "    {\n"
                         + "      \"key\": \"jurisdiction\",\n"
                         + "      \"operator\": \"IN\",\n"
                         + "      \"values\": [ \"IA\" ]\n"
                         + "    }\n"
                         + "  ]\n"
                         + "}\n")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            asList(
                new SearchParameterBoolean(AVAILABLE_TASKS_ONLY, BOOLEAN, false),
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            expectedReq,
            accessControlResponse
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 RELEASE_2_TASK_QUERY,
                 accessControlResponse.getUserInfo().getUid(),
                 IDAM_USER_EMAIL
             )
        ).thenReturn(true);

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"request_context\",\n"
                             + "      \"operator\": \"CONTEXT\",\n"
                             + "      \"value\": \"" + context.toString() + "\"\n"
                             + "    },\n"
                             + "    {\n"
                             + "      \"key\": \"jurisdiction\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [ \"IA\" ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        SearchTaskRequest expectedReq = new SearchTaskRequest(
            asList(
                new SearchParameterRequestContext(REQUEST_CONTEXT, CONTEXT, context),
                new SearchParameterList(JURISDICTION, IN, singletonList("IA"))
            )
        );

        verify(cftQueryService, times(1)).searchForTasks(
            0,
            50,
            expectedReq,
            accessControlResponse
        );
    }

    @Test
    void should_return_a_400_for_invalid_request_context() throws Exception {
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
        //enable R2 flag
        when(launchDarklyFeatureFlagProvider.getBooleanValue(
                 RELEASE_2_TASK_QUERY,
                 accessControlResponse.getUserInfo().getUid(),
                 IDAM_USER_EMAIL
             )
        ).thenReturn(true);

        mockMvc.perform(
            post("/task")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content("{\n"
                             + "  \"search_parameters\": [\n"
                             + "    {\n"
                             + "      \"key\": \"request_context\",\n"
                             + "      \"operator\": \"CONTEXT\",\n"
                             + "      \"value\": \"GENERAL_SEARCH\"\n"
                             + "    },\n"
                             + "    {\n"
                             + "      \"key\": \"jurisdiction\",\n"
                             + "      \"operator\": \"IN\",\n"
                             + "      \"values\": [ \"IA\" ]\n"
                             + "    }\n"
                             + "  ]\n"
                             + "}\n")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isBadRequest());
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

    private List<CamundaVariableInstance> mockedAllVariables(String processInstanceId,
                                                             String jurisdiction,
                                                             String taskId) {

        return asList(
            new CamundaVariableInstance(
                jurisdiction,
                "String",
                "jurisdiction",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "PUBLIC",
                "String",
                "securityClassification",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "Read,Refer,Own,Manager,Cancel",
                "String",
                "tribunal-caseworker",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "caseId1",
                "String",
                "caseId",
                processInstanceId,
                taskId
            )
        );

    }
}

