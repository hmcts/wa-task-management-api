package uk.gov.hmcts.reform.wataskmanagementapi.wataskconfigurationapi.controllers;

import feign.FeignException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.ActorIdType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleCategory;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.AddLocalVariableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.idam.IdamTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.auth.role.entities.request.MultipleQueryRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.controllers.request.ConfigureTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.AssigneeRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DecisionTableRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.request.DmnRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.camunda.response.ConfigurationDmnEvaluationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.DecisionTable.WA_TASK_CONFIGURATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.jsonValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaValue.stringValue;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.TaskState.UNCONFIGURED;

class TaskConfigurationControllerTest extends SpringBootIntegrationBaseTest {

    public static final String TASK_CONFIGURATION_ENDPOINT = "/task-configuration/";
    private static final String TASK_NAME = "taskName";
    private static final String BEARER_SERVICE_TOKEN = "Bearer service token";
    private static final String BEARER_USER_TOKEN = "Bearer user token";
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;
    @MockBean(name = "systemUserIdamToken")
    private IdamTokenGenerator systemUserIdamToken;
    @MockBean
    private CcdDataServiceApi ccdDataServiceApi;
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    private String testTaskId;
    private String testProcessInstanceId;
    private String testUserId;
    private String testCaseId;
    @Mock
    private CaseDetails caseDetails;

    @BeforeEach
    void setUp() {
        testTaskId = UUID.randomUUID().toString();
        testProcessInstanceId = UUID.randomUUID().toString();
        testUserId = UUID.randomUUID().toString();
        testCaseId = UUID.randomUUID().toString();

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));
    }

    @DisplayName("Should return 200 and configure a task over REST with no auto-assign")
    @Test
    void should_succeed_and_configure_a_task_over_rest_with_no_auto_assignment() throws Exception {

        setupRoleAssignmentResponse(false);
        HashMap<String, CamundaValue<String>> modifications = configure3rdPartyResponses();

        mockMvc.perform(
            post(TASK_CONFIGURATION_ENDPOINT + testTaskId)
                .contentType(APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn();


        ArgumentCaptor<AddLocalVariableRequest> argumentCaptor = ArgumentCaptor.forClass(AddLocalVariableRequest.class);
        verify(camundaServiceApi, times(2)).addLocalVariablesToTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(testTaskId),
            argumentCaptor.capture()
        );

        Map<String, CamundaValue<String>> stateUpdate = Map.of(TASK_STATE.value(), stringValue(UNASSIGNED.value()));

        List<AddLocalVariableRequest> capturedArguments = argumentCaptor.getAllValues();
        Assertions.assertEquals(new AddLocalVariableRequest(modifications), capturedArguments.get(0));
        Assertions.assertEquals(new AddLocalVariableRequest(stateUpdate), capturedArguments.get(1));


        verify(camundaServiceApi, never()).assignTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(testTaskId),
            any()
        );
    }

    @DisplayName("Should return 200 and configure a task over REST with auto-assign")
    @Test
    void should_succeed_and_configure_a_task_over_rest_with_auto_assignment() throws Exception {

        setupRoleAssignmentResponse(true);
        HashMap<String, CamundaValue<String>> modifications = configure3rdPartyResponses();

        mockMvc.perform(
            post(TASK_CONFIGURATION_ENDPOINT + testTaskId)
                .contentType(APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn();


        ArgumentCaptor<AddLocalVariableRequest> argumentCaptor = ArgumentCaptor.forClass(AddLocalVariableRequest.class);
        verify(camundaServiceApi, times(2)).addLocalVariablesToTask(
            eq(BEARER_SERVICE_TOKEN),
            eq(testTaskId),
            argumentCaptor.capture()
        );

        Map<String, CamundaValue<String>> stateUpdate = Map.of(TASK_STATE.value(), stringValue(ASSIGNED.value()));

        List<AddLocalVariableRequest> capturedArguments = argumentCaptor.getAllValues();
        Assertions.assertEquals(new AddLocalVariableRequest(modifications), capturedArguments.get(0));
        Assertions.assertEquals(new AddLocalVariableRequest(stateUpdate), capturedArguments.get(1));


        verify(camundaServiceApi, times(1)).assignTask(
            BEARER_SERVICE_TOKEN,
            testTaskId,
            new AssigneeRequest(testUserId)
        );
    }

    @DisplayName("Should return 404 if task did not exist when configuring a task")
    @Test
    void should_fail_and_return_404_when_configuring_a_task_over_rest() throws Exception {

        when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, testTaskId))
            .thenThrow(mock(FeignException.NotFound.class));
        when(systemUserIdamToken.generate()).thenReturn(BEARER_USER_TOKEN);
        when(serviceAuthTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        mockMvc.perform(
            post(TASK_CONFIGURATION_ENDPOINT + testTaskId)
                .contentType(APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isNotFound());

        verify(camundaServiceApi, never()).addLocalVariablesToTask(
            eq(BEARER_SERVICE_TOKEN),
            any(String.class),
            any(AddLocalVariableRequest.class)
        );
    }

    @DisplayName("Should return 200 and return configuration as body with auto-assignment")
    @Test
    void should_succeed_and_return_configuration_with_auto_assignment() throws Exception {

        setupRoleAssignmentResponse(true);
        configure3rdPartyResponses();

        String expectedResponse = "{\n"
                                  + "  \"task_id\": \"" + testTaskId + "\",\n"
                                  + "  \"case_id\": \"" + testCaseId + "\",\n"
                                  + "  \"assignee\": \"" + testUserId + "\",\n"
                                  + "  \"configuration_variables\": {\n"
                                  + "    \"taskType\": \"reviewTheAppeal\",\n"
                                  + "    \"jurisdiction\": \"IA\",\n"
                                  + "    \"caseTypeId\": \"Asylum\",\n"
                                  + "    \"taskState\": \"assigned\",\n"
                                  + "    \"executionType\": \"Case Management Task\",\n"
                                  + "    \"caseId\": \"" + testCaseId + "\",\n"
                                  + "    \"securityClassification\": \"PUBLIC\",\n"
                                  + "    \"autoAssigned\": true,\n"
                                  + "    \"taskSystem\": \"SELF\",\n"
                                  + "    \"title\": \"taskName\""
                                  + "  }\n"
                                  + "}";


        Map<String, Object> requiredProcessVariables = Map.of(
            TASK_ID.value(), "reviewTheAppeal",
            CASE_ID.value(), testCaseId,
            CamundaVariableDefinition.TASK_NAME.value(), TASK_NAME
        );

        mockMvc.perform(
            post(TASK_CONFIGURATION_ENDPOINT + testTaskId + "/configuration")
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(new ConfigureTaskRequest(requiredProcessVariables)))
        )
            .andExpect(status().isOk())
            .andExpect(content().json(expectedResponse))
            .andReturn();

    }

    @DisplayName("Should return 200 and return configuration as body with no auto-assignment")
    @Test
    void should_succeed_and_return_configuration_with_n_auto_assignment() throws Exception {

        setupRoleAssignmentResponse(false);
        configure3rdPartyResponses();

        String expectedResponse = "{\n"
                                  + "  \"task_id\": \"" + testTaskId + "\",\n"
                                  + "  \"case_id\": \"" + testCaseId + "\",\n"
                                  + "  \"configuration_variables\": {\n"
                                  + "    \"taskType\": \"reviewTheAppeal\",\n"
                                  + "    \"jurisdiction\": \"IA\",\n"
                                  + "    \"caseTypeId\": \"Asylum\",\n"
                                  + "    \"taskState\": \"unassigned\",\n"
                                  + "    \"executionType\": \"Case Management Task\",\n"
                                  + "    \"caseId\": \"" + testCaseId + "\",\n"
                                  + "    \"securityClassification\": \"PUBLIC\",\n"
                                  + "    \"autoAssigned\": false,\n"
                                  + "    \"taskSystem\": \"SELF\",\n"
                                  + "    \"title\": \"taskName\""
                                  + "  }\n"
                                  + "}";

        Map<String, Object> requiredProcessVariables = Map.of(
            TASK_ID.value(), "reviewTheAppeal",
            CASE_ID.value(), testCaseId,
            CamundaVariableDefinition.TASK_NAME.value(), TASK_NAME
        );

        mockMvc.perform(
            post(TASK_CONFIGURATION_ENDPOINT + testTaskId + "/configuration")
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(new ConfigureTaskRequest(requiredProcessVariables)))
        )
            .andExpect(status().isOk())
            .andExpect(content().json(expectedResponse))
            .andReturn();
    }

    private void setupRoleAssignmentResponse(boolean shouldReturnRoleAssignment) {
        Function<Boolean, List<RoleAssignment>> getRoleAssignment = (condition) ->
            (condition) ? List.of(RoleAssignment.builder()
                .id("someId")
                .actorIdType(ActorIdType.IDAM)
                .actorId(testUserId)
                .roleName("tribunal-caseworker")
                .roleCategory(RoleCategory.LEGAL_OPERATIONS)
                .roleType(RoleType.ORGANISATION)
                .classification(Classification.PUBLIC)
                .build()) : emptyList();

        when(roleAssignmentServiceApi.queryRoleAssignments(
            eq(BEARER_USER_TOKEN),
            eq(BEARER_SERVICE_TOKEN),
            any(MultipleQueryRequest.class)
        )).thenReturn(new RoleAssignmentResource(getRoleAssignment.apply(shouldReturnRoleAssignment)));

    }

    private HashMap<String, CamundaValue<String>> configure3rdPartyResponses() {

        when(camundaServiceApi.getTask(BEARER_SERVICE_TOKEN, testTaskId))
            .thenReturn(new CamundaTask(testTaskId, testProcessInstanceId, TASK_NAME));

        Map<String, CamundaValue<Object>> processVariables = Map.of(
            TASK_ID.value(), new CamundaValue<>("reviewTheAppeal", "String"),
            CASE_ID.value(), new CamundaValue<>(testCaseId, "String"),
            TASK_STATE.value(), new CamundaValue<>(UNCONFIGURED, "String")
        );

        when(camundaServiceApi.getVariables(BEARER_SERVICE_TOKEN, testTaskId))
            .thenReturn(processVariables);

        when(idamWebApi.token(ArgumentMatchers.<Map<String, Object>>any()))
            .thenReturn(new Token(BEARER_USER_TOKEN, "scope"));

        when(systemUserIdamToken.generate()).thenReturn(BEARER_USER_TOKEN);
        when(serviceAuthTokenGenerator.generate()).thenReturn(BEARER_SERVICE_TOKEN);

        when(ccdDataServiceApi.getCase(
            BEARER_USER_TOKEN,
            BEARER_SERVICE_TOKEN,
            testCaseId
            )
        ).thenReturn(caseDetails);


        when(camundaServiceApi.evaluateConfigurationDmnTable(
            BEARER_SERVICE_TOKEN,
            WA_TASK_CONFIGURATION.getTableKey("ia", "asylum"),
            "ia",
            new DmnRequest<>(new DecisionTableRequest(jsonValue(caseDetails.toString())))
        )).thenReturn(singletonList(new ConfigurationDmnEvaluationResponse(stringValue("name"), stringValue("value1"))));

        HashMap<String, CamundaValue<String>> modifications = new HashMap<>();
        modifications.put("caseId", stringValue(testCaseId));
        modifications.put("taskState", stringValue("configured"));
        modifications.put("autoAssigned", stringValue("false"));
        modifications.put("caseTypeId", stringValue("Asylum"));
        modifications.put("executionType", stringValue("Case Management Task"));
        modifications.put("securityClassification", stringValue("PUBLIC"));
        modifications.put("taskSystem", stringValue("SELF"));
        modifications.put("jurisdiction", stringValue("IA"));
        modifications.put("title", stringValue(TASK_NAME));
        modifications.put("taskType", stringValue("reviewTheAppeal"));

        return modifications;
    }
}
