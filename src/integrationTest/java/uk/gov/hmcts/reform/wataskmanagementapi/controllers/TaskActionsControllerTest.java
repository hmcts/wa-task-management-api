package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.Assignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.GetRoleAssignmentResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.SearchTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTaskCount;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.Warning;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.task.WarningValues;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;

class TaskActionsControllerTest extends SpringBootIntegrationBaseTest {

    @Autowired
    protected AuthorizationHeadersProvider authorizationHeadersProvider;
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @Autowired
    private ObjectMapper objectMapper;

    private ServiceMocks mockServices;

    @BeforeEach
    public void setUp() {
        mockServices = new ServiceMocks(idamWebApi,
                                        camundaServiceApi,
                                        roleAssignmentServiceApi);
    }

    @Nested
    @DisplayName("cancelTask()")
    class CancelTask {

        @DisplayName("Invalid escalation code")
        @Test
        void should_return_a_500_when_esclation_code_is_invalid() throws Exception {
            final String taskId = UUID.randomUUID().toString();

            final String errorMessage = "There was a problem cancelling "
                                        + "the task with id: " + taskId;

            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", String.format(
                        "There was a problem cancelling the task with id: %s",
                        taskId)));
            doThrow(mockFeignException).when(camundaServiceApi).bpmnEscalation(any(), any(), any());

            mockMvc.perform(
                post("/task/" + taskId + "/cancel")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
                .andExpect(result -> assertEquals(
                    errorMessage,
                    result.getResolvedException().getMessage()
                ));

            verify(camundaServiceApi, times(1))
                .bpmnEscalation(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("getTask()")
    class GetTask {
        @Test
        void should_return_a_500_when_id_invalid() throws Exception {
            final String taskId = UUID.randomUUID().toString();

            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.FeignServerException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", String.format(
                        "There was a problem fetching the task with id: %s",
                        taskId
                    )));
            doThrow(mockFeignException).when(roleAssignmentServiceApi).createRoleAssignment(any(), any(), any());

            mockMvc.perform(
                get("/task/" + taskId)
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError());

            verify(camundaServiceApi, times(1))
                .getTask(any(), any());
        }


        @Test
        void should_return_a_400_when_restricted_role_is_given() throws Exception {
            final String taskId = UUID.randomUUID().toString();

            final String userToken = "user_token";

            mockServices.mockUserInfo();

            final List<String> roleNames = asList("tribunal-caseworker");

            Map<String, String> roleAttributes = new HashMap<>();
            roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

            // Role attribute is IA
            List<Assignment> allTestRoles = new ArrayList<>();
            roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
                .forEach(roleType -> {
                    Assignment roleAssignment = mockServices.createBaseAssignment(
                        UUID.randomUUID().toString(), "tribunal-caseworker",
                        roleType,
                        Classification.PUBLIC,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }));

            GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
                allTestRoles
            );
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(userToken, "scope"));

            // Task created with Jurisdiction SSCS
            mockCamundaVariables();

            mockMvc.perform(
                get("/task/" + taskId)
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().isForbidden());

        }

        @Test
        void should_return_a_task_with_warning_when_has_warnings_is_given() throws Exception {


            final String userToken = "user_token";

            mockServices.mockUserInfo();

            final List<String> roleNames = asList("tribunal-caseworker");

            Map<String, String> roleAttributes = new HashMap<>();
            roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

            // Role attribute is IA
            List<Assignment> allTestRoles = new ArrayList<>();
            roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
                .forEach(roleType -> {
                    Assignment roleAssignment = mockServices.createBaseAssignment(
                        UUID.randomUUID().toString(), "tribunal-caseworker",
                        roleType,
                        Classification.PUBLIC,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }));

            GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
                allTestRoles
            );
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            final String taskId = UUID.randomUUID().toString();

            final var warningList = new WarningValues(asList(new Warning("TA01", "Description")));
            CamundaTask task = new CamundaTask(taskId,
                                               "name",
                                               "assignee",
                                               ZonedDateTime.now(),
                                               ZonedDateTime.now(),
                                               "description",
                                               "owner",
                                               "formkey",
                                               "processInstanceId");

            when(idamWebApi.token(any())).thenReturn(new Token(userToken, "scope"));
            when(camundaServiceApi.getTask(any(), any()))
                .thenReturn(task);

            mockCamundaVariablesWithWarning();

            mockMvc.perform(
                get("/task/" + taskId)
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(jsonPath("$.task.id").value(taskId))
                .andExpect(jsonPath("$.task.warnings").value(true))
                .andExpect(jsonPath("$.task.warning_list.values").isArray())
                .andExpect(jsonPath("$.task.warning_list.values",hasSize(1)))
                .andExpect(jsonPath("$.task.warning_list.values[0].warningCode").value("TA01"))
                .andExpect(jsonPath("$.task.warning_list.values[0].warningText").value("Description"));
        }

        private void mockCamundaVariables() {
            Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

            processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
            processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));
            processVariables.put("jurisdiction", new CamundaVariable("SCSS", "string"));

            when(camundaServiceApi.getVariables(any(), any()))
                .thenReturn(processVariables);
        }

        private void mockCamundaVariablesWithWarning() {
            Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

            processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
            processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));
            processVariables.put("jurisdiction", new CamundaVariable("IA", "string"));
            processVariables.put("hasWarnings", new CamundaVariable(true, "Boolean"));

            WarningValues warningValues = new WarningValues(Arrays.asList(new Warning("TA01","Description")));
            Gson gson = new Gson();
            var values = gson.toJson(warningValues.getValues());
            processVariables.put("warningList", new CamundaVariable(values, "WarningValues"));

            when(camundaServiceApi.getVariables(any(), any()))
                .thenReturn(processVariables);
        }

    }

    @Nested
    @DisplayName("taskSearch()")
    class SearchTask {

        @ParameterizedTest
        @ValueSource(strings = {
            "/task", "/task?first_result=0", "/task?max_results=1", "/task?first_result=0&max_results=1"
        })
        void should_return_a_200_when_restricted_role_is_given(String uri) throws Exception {
            final String taskId = UUID.randomUUID().toString();

            final String userToken = "user_token";

            mockServices.mockUserInfo();

            final List<String> roleNames = singletonList("tribunal-caseworker");

            // Role attribute is IA
            Map<String, String> roleAttributes = new HashMap<>();
            roleAttributes.put(RoleAttributeDefinition.JURISDICTION.value(), "IA");

            List<Assignment> allTestRoles = new ArrayList<>();
            roleNames.forEach(roleName -> asList(RoleType.ORGANISATION, RoleType.CASE)
                .forEach(roleType -> {
                    Assignment roleAssignment = mockServices.createBaseAssignment(
                        UUID.randomUUID().toString(), "tribunal-caseworker",
                        roleType,
                        Classification.PUBLIC,
                        roleAttributes
                    );
                    allTestRoles.add(roleAssignment);
                }));

            GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
                allTestRoles
            );
            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(userToken, "scope"));

            List<CamundaTask> camundaTasks = List.of(mockServices.getCamundaTask("processInstanceId", "some-id"));
            when(camundaServiceApi.searchWithCriteriaAndPagination(
                any(), anyInt(), anyInt(), any())).thenReturn(camundaTasks);

            // Task created with Jurisdiction SSCS
            when(camundaServiceApi.getAllVariables(any(), any()))
                .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

            SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
                new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
            ));

            final String searchContent = objectMapper.writeValueAsString(searchTaskRequest);
            mockMvc.perform(
                post(uri)
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .content(searchContent)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(
                ResultMatcher.matchAll(
                    status().isOk(),
                    jsonPath("total_records").value(0),
                    jsonPath("$.tasks").isEmpty())
            );
        }

        /*
            Single Task is created with two role assignments one with IA and Organisation and
            other with SSCS and Case.
            When a task is searched with SSCS , test returns only single result with SSCS Jurisdiction
         */
        @Test
        void should_return_single_task_when_two_role_assignments_with_one_restricted_is_given() throws Exception {
            final String taskId = UUID.randomUUID().toString();

            final String userToken = "user_token";

            mockServices.mockUserInfo();

            // create role assignments with IA, Organisation and SCSS , Case
            GetRoleAssignmentResponse accessControlResponse = new GetRoleAssignmentResponse(
                mockServices.createRoleAssignmentsWithSCSSandIA()
            );

            when(roleAssignmentServiceApi.getRolesForUser(
                any(), any(), any()
            )).thenReturn(accessControlResponse);

            when(idamWebApi.token(any())).thenReturn(new Token(userToken, "scope"));

            List<CamundaTask> camundaTasks = List.of(mockServices.getCamundaTask("processInstanceId", taskId));
            when(camundaServiceApi.searchWithCriteriaAndPagination(
                any(), anyInt(), anyInt(), any())).thenReturn(camundaTasks);

            when(camundaServiceApi.getTaskCount(any(), any())).thenReturn(new CamundaTaskCount(1));

            // Task created with Jurisdiction SCSS
            when(camundaServiceApi.getAllVariables(any(), any()))
                .thenReturn(mockedAllVariables("processInstanceId", "SSCS", taskId));

            SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
                new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
            ));

            final String searchContent = objectMapper.writeValueAsString(searchTaskRequest);
            mockMvc.perform(
                post("/task")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .content(searchContent)
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

    @Nested
    @DisplayName("auto-complete()")
    class AutoCompleteTask {

        @DisplayName("Invalid DMN table")
        @Test
        void should_return_a_500_when_dmn_table_is_invalid() throws Exception {
            final String taskId = UUID.randomUUID().toString();

            final String errorMessage = "There was a problem cancelling "
                                        + "the task with id: " + taskId;

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "some-caseId",
                "some-eventId",
                "ia",
                "asylum"
            );
            ObjectMapper mapper = new ObjectMapper();
            final String searchContent = mapper.writeValueAsString(searchEventAndCase);

            mockServices.mockServiceAPIs();

            FeignException mockFeignException = mock(FeignException.class);

            when(mockFeignException.contentUTF8())
                .thenReturn(mockServices.createCamundaTestException(
                    "aCamundaErrorType", "There was a problem evaluating DMN"));
            doThrow(mockFeignException).when(camundaServiceApi).evaluateDMN(any(), any(), any());

            mockMvc.perform(
                post("/task/search-for-completable")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .content(searchContent)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
                .andExpect(result -> assertEquals(
                    "There was a problem evaluating DMN",
                    result.getResolvedException().getMessage()
                ));

            verify(camundaServiceApi, times(1))
                .evaluateDMN(any(), any(), any());
        }
    }
}
