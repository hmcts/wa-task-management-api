package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
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
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameter;
import uk.gov.hmcts.reform.wataskmanagementapi.services.AuthorizationHeadersProvider;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

class TaskControllerTest extends SpringBootIntegrationBaseTest {

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
            final var taskId = UUID.randomUUID().toString();

            final var errorMessage = "There was a problem cancelling "
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
            final var taskId = UUID.randomUUID().toString();

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
            final var taskId = UUID.randomUUID().toString();

            final var userToken = "user_token";

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

            // Task created with Jurisdiction SCSS
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

        private void mockCamundaVariables() {
            Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

            processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
            processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));
            processVariables.put("jurisdiction", new CamundaVariable("SCSS", "string"));

            when(camundaServiceApi.getVariables(any(), any()))
                .thenReturn(processVariables);
        }

    }

    @Nested
    @DisplayName("taskSearch()")
    class SearchTask {

        @Test
        void should_return_a_200_when_restricted_role_is_given() throws Exception {
            final var taskId = UUID.randomUUID().toString();

            final var userToken = "user_token";

            mockServices.mockUserInfo();

            final List<String> roleNames = asList("tribunal-caseworker");

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

            CamundaTask camundaTask = new CamundaTask(
                "some-id",
                "some-name",
                "some-assignee",
                ZonedDateTime.now(),
                ZonedDateTime.now(),
                "some-description",
                "some-owner",
                "formKey",
                "processInstanceId"
            );

            List<CamundaTask> camundaTasks = List.of(camundaTask);
            when(camundaServiceApi.searchWithCriteria(any(), any())).thenReturn(camundaTasks);

            // Task created with Jurisdiction SCSS
            when(camundaServiceApi.getAllVariables(any(), any())).thenReturn(mockedAllVariables("processInstanceId"));

            SearchTaskRequest searchTaskRequest = new SearchTaskRequest(singletonList(
                new SearchParameter(JURISDICTION, SearchOperator.IN, singletonList("SSCS"))
            ));

            final var searchContent = objectMapper.writeValueAsString(searchTaskRequest);
            mockMvc.perform(
                post("/task")
                    .header(
                        "Authorization",
                        authorizationHeadersProvider.getTribunalCaseworkerAAuthorization()
                    )
                    .content(searchContent)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(ResultMatcher.matchAll(status().isOk(), jsonPath("$.tasks").isEmpty()));

        }

        private List<CamundaVariableInstance> mockedAllVariables(String processInstanceId) {
            Map<String, CamundaVariable> mockVariables = new HashMap<>();
            mockVariables.put("jurisdiction", new CamundaVariable("SCSS", "String"));
            mockVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));
            mockVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
            return mockVariables.keySet().stream()
                .map(
                    mockVarKey ->
                        new CamundaVariableInstance(
                            mockVariables.get(mockVarKey).getValue(),
                            mockVariables.get(mockVarKey).getType(),
                            mockVarKey,
                            processInstanceId
                        ))
                .collect(Collectors.toList());

        }

    }

    @Nested
    @DisplayName("auto-complete()")
    class AutoCompleteTask {

        @DisplayName("Invalid DMN table")
        @Test
        void should_return_a_500_when_dmn_table_is_invalid() throws Exception {
            final var taskId = UUID.randomUUID().toString();

            final var errorMessage = "There was a problem cancelling "
                                     + "the task with id: " + taskId;

            SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
                "some-caseId",
                "some-eventId",
                "ia",
                "asylum"
            );
            ObjectMapper mapper = new ObjectMapper();
            final var searchContent = mapper.writeValueAsString(searchEventAndCase);

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
