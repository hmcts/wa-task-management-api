package uk.gov.hmcts.reform.wataskmanagementapi.controllers.newinitiate;

import feign.FeignException;
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
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAttributeDefinition;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.Classification;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.enums.RoleType;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.InitiateTaskRequestNew;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTime;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.clients.CcdDataServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.taskconfiguration.domain.entities.ccd.CaseDetails;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CASE_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.DUE_DATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_NAME;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TASK_TYPE;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.TITLE;
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
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Mock
    private UserInfo mockedUserInfo;


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
    void should_return_a_500_when_id_invalid() throws Exception {

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
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().is5xxServerError());

        verify(camundaServiceApi, times(1))
            .getTask(any(), any());
    }

    @Test
    void should_return_a_403_when_restricted_role_is_given() throws Exception {

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
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));

        // Task created with Jurisdiction SSCS
        mockCamundaVariables();

        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isForbidden());

    }

    @Test
    public void should_return_a_403_when_the_user_jurisdiction_did_not_match() throws Exception {
        insertDummyTaskInDb(taskId);

        mockServices.mockUserInfo();

        // create role assignments Organisation and SCSS and Case Id
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(
            mockServices.createRoleAssignmentsWithJurisdiction("SCSS", "caseId1")
        );

        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);


        mockMvc.perform(
            get("/task/" + taskId)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value(
                "https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
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
        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(
                           FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
                           IDAM_USER_ID,
                           IDAM_USER_EMAIL
                       )
        ).thenReturn(true);

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
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);

        when(caseDetails.getCaseType()).thenReturn("Asylum");
        when(caseDetails.getJurisdiction()).thenReturn("IA");
        when(caseDetails.getSecurityClassification()).thenReturn(("PUBLIC"));

        when(ccdDataServiceApi.getCase(any(), any(), eq("someCaseId")))
            .thenThrow(new RuntimeException("some error"));

        ZonedDateTime dueDate = ZonedDateTime.now().plusDays(1);
        String formattedDueDate = CamundaTime.CAMUNDA_DATA_TIME_FORMATTER.format(dueDate);

        Map<String, Object> taskAttributes = Map.of(
            TASK_TYPE.value(), "followUpOverdueReasonsForAppeal",
            TASK_NAME.value(), "follow Up Overdue Reasons For Appeal",
            TITLE.value(), "A test task",
            CASE_ID.value(), "someCaseId",
            DUE_DATE.value(), formattedDueDate
        );

        InitiateTaskRequestNew req = new InitiateTaskRequestNew(INITIATION, taskAttributes);

        //first initiate call
        mockMvc
            .perform(post("/task/" + taskId)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
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
            .perform(post("/task/" + taskId)
                         .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                         .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                         .contentType(APPLICATION_JSON_VALUE)
                         .content(asJsonString(req)))
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

    private void mockCamundaVariables() {
        Map<String, CamundaVariable> processVariables = new ConcurrentHashMap<>();

        processVariables.put("tribunal-caseworker", new CamundaVariable("Read,Refer,Own,Manager,Cancel", "string"));
        processVariables.put("securityClassification", new CamundaVariable("PUBLIC", "string"));
        processVariables.put("jurisdiction", new CamundaVariable("SCSS", "string"));

        when(camundaServiceApi.getVariables(any(), any()))
            .thenReturn(processVariables);
    }

    private void insertDummyTaskInDb(String taskId) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction("IA");
        taskResource.setCaseTypeId("Asylum");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("caseId1");


        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, true, true, true, true,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        tribunalResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }


}

