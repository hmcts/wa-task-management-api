package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
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
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.search.SearchParameterKey.JURISDICTION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
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

    private ServiceMocks mockServices;

    @BeforeEach
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi);
    }

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
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
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

