package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
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
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
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
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag.RELEASE_2_TASK_QUERY;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.WORK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
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
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("access_requests"))
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
            IDAM_USER_EMAIL)
        ).thenReturn(true);

        SearchTaskRequest searchTaskRequest = new SearchTaskRequest(asList(
            new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("IA")),
            new SearchParameter(WORK_TYPE, SearchOperator.IN, singletonList("invalid_value"))
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
                               + "error_management]")));
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
            .andExpect(
                ResultMatcher.matchAll(
                    status().isBadRequest(),
                    content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                    jsonPath("$.type")
                        .value("https://github.com/hmcts/wa-task-management-api/problem/bad-request"),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Unexpected end-of-input: expected close marker for Object "
                               + "(start marker at [Source: (PushbackInputStream); line: 1, column: 1])")));
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
                               + "double-quote to start field name")));
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
                        .value("Invalid request field: search_parameters.[0].key")));
    }

    @Test
    void should_return_400_bad_request_when_invalid_camelCase_worktype_search_parameter_key() throws Exception {


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
                        .value("Invalid request field: search_parameters.[0].key")));
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
                    jsonPath("$.detail").value("Invalid request field: search_parameters.[0].operator")));
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
                        .value("At least one search_parameter element is required.")));
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
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations").isNotEmpty(),
                    jsonPath("$.violations.[0].field").value("search_parameters[0].operator"),
                    jsonPath("$.violations.[0].message")
                        .value("Each search_parameter element must have 'key', 'values'"
                               + " and 'operator' fields present and populated.")));
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
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/bad-request"
                        ),
                    jsonPath("$.title").value("Bad Request"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.detail")
                        .value("Invalid request field: search_parameters.[0].operator")));
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
                        .value(
                            "https://github.com/hmcts/wa-task-management-api/problem/constraint-validation"
                        ),
                    jsonPath("$.title").value("Constraint Violation"),
                    jsonPath("$.status").value(400),
                    jsonPath("$.violations").isNotEmpty(),
                    jsonPath("$.violations.[0].field").value("search_parameters[0].operator"),
                    jsonPath("$.violations.[0].message")
                        .value("Each search_parameter element must have 'key', 'values' and 'operator' fields present and populated.")));
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
                               + "and 'operator' fields present and populated.")));
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
                        .value("Invalid request field: search_parameters.[0].key")));
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
            IDAM_USER_EMAIL)
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
            IDAM_USER_EMAIL)
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

