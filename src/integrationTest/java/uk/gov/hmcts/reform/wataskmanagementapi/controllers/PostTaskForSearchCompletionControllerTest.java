package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.AllowedJurisdictionConfiguration;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
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
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
class PostTaskForSearchCompletionControllerTest {

    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Mock
    private UserInfo mockedUserInfo;
    @Mock
    private AllowedJurisdictionConfiguration allowedJurisdictionConfiguration;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    IntegrationTestUtils integrationTestUtils;
    RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();
    private String taskId;
    private ServiceMocks mockServices;

    private SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
        "some-caseId",
        "decideAnApplication",
        "ia",
        "asylum"
    );

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @BeforeEach
    void beforeEach() {
        taskId = UUID.randomUUID().toString();

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);
    }

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @DisplayName("Invalid DMN table")
    @Test
    void should_return_a_500_when_dmn_table_is_invalid() throws Exception {
        searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "some-eventId",
            "ia",
            "asylum"
        );
        mockServices.mockServiceAPIs();

        FeignException mockFeignException = mock(FeignException.class);
        when(mockFeignException.contentUTF8())
            .thenReturn(mockServices.createCamundaTestException(
                "aCamundaErrorType", "There was a problem evaluating DMN"));
        doThrow(mockFeignException).when(camundaServiceApi).evaluateDMN(any(), any(), any(), any());

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andExpect(status().is5xxServerError())
            .andExpect(result -> assertEquals(
                "There was a problem evaluating DMN",
                result.getResolvedException().getMessage()
            ));

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any(), any());
    }

    @Test
    void should_return_a_200_when_dmn_table_is_valid() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content("{\"case_id\":\"some-caseId\",\"event_id\":\"decideAnApplication\","
                                 + "\"case_jurisdiction\":\"ia\",\"case_type\":\"asylum\"}")
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").isNotEmpty());

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any(), any());
    }

    @Test
    void should_return_a_200_and_empty_list_when_jurisdiction_not_IA_and_case_type_not_asylum() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "decideAnApplication",
            "SSCS",
            "aCaseType"
        );

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_empty_list_when_jurisdiction_and_case_type_not_supported_for_both_flows()
        throws Exception {
        String caseId = "searchForCompletableApiFirstCaseId1";
        String eventId = "caseworker-issue-case";
        String jurisdiction = "invalidJurisdiction";
        String caseType = "invalidCaseType";
        String apiFirstTaskId = UUID.randomUUID().toString();
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            eventId,
            jurisdiction,
            caseType
        );
        mockServices.mockUserInfo();

        List<RoleAssignment> roleAssignments = createStandardRoleAssignments(caseId, jurisdiction, caseType);
        TaskRoleResource taskRoleResource = createTaskRoleResourceForCompletableSearch();
        insertApiFirstTaskInDb(caseId, apiFirstTaskId, jurisdiction, caseType, eventId, true, taskRoleResource);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any())).thenReturn(accessControlResponse);
        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0))
            .andExpect(jsonPath("task_required_for_event").value(false));

        verify(camundaServiceApi, times(0)).evaluateDMN(any(), any(), any(), anyMap());
    }

    @Test
    void should_return_a_200_and_empty_list_even_with_optional_api_first_rule_when_not_supported()
        throws Exception {
        String caseId = "searchForCompletableApiFirstCaseId2";
        String eventId = "caseworker-send-order";
        String jurisdiction = "invalidJurisdiction";
        String caseType = "invalidCaseType";
        String apiFirstTaskId = UUID.randomUUID().toString();
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            eventId,
            jurisdiction,
            caseType
        );
        mockServices.mockUserInfo();

        List<RoleAssignment> roleAssignments = createStandardRoleAssignments(caseId, jurisdiction, caseType);
        TaskRoleResource taskRoleResource = createTaskRoleResourceForCompletableSearch();
        insertApiFirstTaskInDb(caseId, apiFirstTaskId, jurisdiction, caseType, eventId, false, taskRoleResource);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any())).thenReturn(accessControlResponse);
        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0))
            .andExpect(jsonPath("task_required_for_event").value(false));

        verify(camundaServiceApi, times(0)).evaluateDMN(any(), any(), any(), anyMap());
    }

    @Test
    void should_return_a_200_and_empty_list_when_idam_user_id_different_from_task_assignee() throws Exception {
        mockServices.mockUserInfo();
        mockServices.mockServiceAPIs();

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_task_list_when_idam_user_id_same_with_task_assignee() throws Exception {
        String caseId = "searchForCompletableCaseId1";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            "ia",
            "asylum"
        );
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
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
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

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String"),
            "description", new CamundaVariable("aDescription", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables(caseId, "processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1))
            .andExpect(jsonPath("tasks[0].assignee").value("IDAM_USER_ID"))
            .andExpect(jsonPath("tasks[0].description").value("aDescription"));
    }

    @Test
    void should_return_task_with_granular_permissions_when_granular_permission_flag_on() throws Exception {
        String caseId = "searchForCompletableCaseId2";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            "ia",
            "asylum"
        );
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
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name(),
            taskId, OffsetDateTime.now(), true, true, true, true,
            true, true, true, true, true, false
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

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String"),
            "description", new CamundaVariable("aDescription", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables(caseId, "processInstanceId", "IA", taskId));

        mockMvc.perform(
            post("/task/search-for-completable")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .content(integrationTestUtils.asJsonString(searchEventAndCase))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("$.tasks").isNotEmpty(),
            jsonPath("$.tasks[0].assignee").value("IDAM_USER_ID"),
            jsonPath("$.tasks[0].description").value("aDescription"),
            jsonPath("$.tasks[0].permissions.values[0]").value("Own"),
            jsonPath("$.tasks[0].permissions.values[1]").value("Execute"),
            jsonPath("$.tasks[0].permissions.values[2]").value("Complete"),
            jsonPath("$.tasks[0].permissions.values[3]").value("CompleteOwn"),
            jsonPath("$.tasks[0].permissions.values[4]").value("CancelOwn"),
            jsonPath("$.tasks[0].permissions.values[5]").value("Claim"),
            jsonPath("$.tasks[0].permissions.values[6]").value("Unclaim"),
            jsonPath("$.tasks[0].permissions.values[7]").value("Assign"),
            jsonPath("$.tasks[0].permissions.values[8]").value("Unassign"),
            jsonPath("$.tasks[0].permissions.values[9]").value("UnclaimAssign"),
            jsonPath("$.tasks[0].permissions.values[10]").value("UnassignClaim"),
            jsonPath("$.tasks[0].permissions.values.length()").value(11)
        ).andReturn();
    }

    @Test
    void should_return_a_200_and_task_list_with_warnings_and_additional_properties() throws Exception {
        String caseId = "searchForCompletableCaseId22";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            "ia",
            "asylum"
        );
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
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        String roleAssignmentId = UUID.randomUUID().toString();

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskWithWarningsAndAdditionalPropertiesInDb(caseId, taskId, "IA", "Asylum",
                                                               roleAssignmentId, taskRoleResource
        );
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String"),
            "description", new CamundaVariable("aDescription", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables(caseId, "processInstanceId", "IA", taskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1))
            .andExpect(jsonPath("tasks[0].warning_list.values.size()").value(2))
            .andExpect(jsonPath("tasks[0].warning_list.values[0].warningCode").value("Code1"))
            .andExpect(jsonPath("tasks[0].warning_list.values[0].warningText").value("Text1"))
            .andExpect(jsonPath("tasks[0].warning_list.values[1].warningCode").value("Code2"))
            .andExpect(jsonPath("tasks[0].warning_list.values[1].warningText").value("Text2"))
            .andExpect(jsonPath("tasks[0].additional_properties['roleAssignmentId']").value(roleAssignmentId));
    }

    @Test
    void should_return_a_200_and_empty_list_when_task_does_not_have_required_permissions() throws Exception {
        String caseId = "searchForCompletableCaseId3";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnAppllication",
            "ia",
            "asylum"
        );
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
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, false, false, true, false, false,
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

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));
    }

    @Test
    void should_return_a_200_and_retrieve_single_task_when_one_of_the_task_does_not_have_required_permissions()
        throws Exception {
        String caseId = "searchForCompletableCaseId4";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            "ia",
            "asylum"
        );
        mockServices.mockUserInfo();


        List<Map<String, CamundaVariable>> mockedResponse = asList(Map.of(
            "taskType", new CamundaVariable("reviewTheAppeal", "String"),
            "completionMode", new CamundaVariable("Auto", "String"),
            "workType", new CamundaVariable("decision_making_work", "String"),
            "description", new CamundaVariable("aDescription", "String")
        ));
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(mockedResponse);

        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables(caseId, "processInstanceId", "IA", taskId));

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
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(caseId, taskId, "IA", "Asylum", taskRoleResource);

        taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, false, false, true, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
        insertDummyTaskInDb(caseId, UUID.randomUUID().toString(), "IA", "Asylum", taskRoleResource);

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            roleAssignments
        );
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(1));
    }

    @Test
    void should_return_a_200_and_merge_camunda_and_api_first_tasks() throws Exception {
        String caseId = "searchForCompletableApiFirstCaseId3";
        String eventId = "decideAnApplication";
        String camundaTaskId = UUID.randomUUID().toString();
        String apiFirstTaskId = UUID.randomUUID().toString();
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            eventId,
            "ia",
            "asylum"
        );
        mockServices.mockUserInfo();

        List<RoleAssignment> roleAssignments = createStandardRoleAssignments(caseId, "IA", "Asylum");
        TaskRoleResource taskRoleResource = createTaskRoleResourceForCompletableSearch();
        insertDummyTaskInDb(caseId, camundaTaskId, "IA", "Asylum", taskRoleResource);
        insertApiFirstTaskInDb(
            caseId,
            apiFirstTaskId,
            "IA",
            "Asylum",
            eventId,
            true,
            createTaskRoleResourceForCompletableSearch()
        );

        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        when(roleAssignmentServiceApi.getRolesForUser(any(), any(), any())).thenReturn(accessControlResponse);
        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);
        when(camundaServiceApi.evaluateDMN(any(), any(), any(), anyMap()))
            .thenReturn(asList(Map.of(
                "taskType", new CamundaVariable("reviewTheAppeal", "String"),
                "completionMode", new CamundaVariable("Auto", "String")
            )));
        when(camundaServiceApi.getAllVariables(any(), any()))
            .thenReturn(mockedAllVariables(caseId, "processInstanceId", "IA", camundaTaskId));

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(2))
            .andExpect(jsonPath("task_required_for_event").value(true));
    }

    @Test
    void should_return_a_200_with_empty_list_when_the_user_did_not_have_any_roles() throws Exception {

        String caseId = "searchForCompletableCaseId5";
        searchEventAndCase = new SearchEventAndCase(
            caseId,
            "decideAnApplication",
            "ia",
            "asylum"
        );
        mockServices.mockUserInfo();

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(new RoleAssignmentResource(emptyList()));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("tasks.size()").value(0));

    }

    @DisplayName("Should return 502 when camunda service is down")
    @Test
    void should_return_status_code_502_when_camunda_service_is_down() throws Exception {

        Request request = Request.create(Request.HttpMethod.POST, "url",
                                         new HashMap<>(), null, new RequestTemplate());

        searchEventAndCase = new SearchEventAndCase(
            "some-caseId",
            "some-eventId",
            "ia",
            "asylum"
        );
        mockServices.mockServiceAPIs();

        FeignException exception = new FeignException.BadGateway(
            "Camunda is down.",
            request,
            null,
            null);

        doThrow(exception)
            .when(camundaServiceApi)
            .evaluateDMN(any(), any(), any(), any());

        when(allowedJurisdictionConfiguration.getAllowedJurisdictions())
            .thenReturn(List.of("wa", "ia", "sscs", "civil"));

        when(allowedJurisdictionConfiguration.getAllowedCaseTypes())
            .thenReturn(List.of("asylum", "wacasetype", "sscs", "civil"));

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);

        mockMvc.perform(
                post("/task/search-for-completable")
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .content(integrationTestUtils.asJsonString(searchEventAndCase))
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
                        + "and the request could not be completed. Message from downstream system: Camunda is down.")
            );

        verify(camundaServiceApi, times(1))
            .evaluateDMN(any(), any(), any(), any());
    }

    private List<CamundaVariableInstance> mockedAllVariables(String caseId, String processInstanceId,
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
                caseId,
                "String",
                "caseId",
                processInstanceId,
                taskId
            ),
            new CamundaVariableInstance(
                "aDescription",
                "String",
                "description",
                processInstanceId,
                taskId
            )
        );

    }

    private void insertDummyTaskInDb(String caseId, String taskId, String jurisdiction, String caseType,
                                     TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "aTaskName",
            "reviewTheAppeal",
            ASSIGNED
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
        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void insertApiFirstTaskInDb(String caseId,
                                        String taskId,
                                        String jurisdiction,
                                        String caseType,
                                        String eventId,
                                        boolean requiredForEvent,
                                        TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "anApiFirstTaskName",
            "anApiFirstTaskType",
            ASSIGNED
        );
        taskResource.setDescription("anApiFirstDescription");
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
        taskResource.setCamundaTask(false);
        taskResource.setCompletionRules(Map.of(eventId, requiredForEvent));
        taskResource.setWorkTypeResource(new WorkTypeResource("decision_making_work", "Decision Making work"));
        taskRoleResource.setTaskId(taskId);
        taskResource.setTaskRoleResources(Set.of(taskRoleResource));
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private List<RoleAssignment> createStandardRoleAssignments(String caseId, String jurisdiction, String caseType) {
        List<RoleAssignment> roleAssignments = new ArrayList<>();
        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction(jurisdiction)
                    .caseType(caseType)
                    .caseId(caseId)
                    .build()
            )
            .build();
        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        return roleAssignments;
    }

    private TaskRoleResource createTaskRoleResourceForCompletableSearch() {
        return new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            false, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );
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
            new NoteResource("Code1", "WARNING", null, "Text1"),
            new NoteResource("Code2", "WARNING", null, "Text2")
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
