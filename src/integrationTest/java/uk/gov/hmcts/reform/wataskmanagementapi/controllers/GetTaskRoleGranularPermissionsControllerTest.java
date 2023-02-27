package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.SearchEventAndCase;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariable;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.WorkTypeResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.EntityManager;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class GetTaskRoleGranularPermissionsControllerTest extends SpringBootIntegrationBaseTest {

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private AuthTokenGenerator serviceAuthTokenGenerator;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @SpyBean
    private CftQueryService cftQueryService;
    @Mock
    private UserInfo mockedUserInfo;
    @MockBean
    @Autowired
    private EntityManager entityManager;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @Mock
    private ServiceMocks mockServices;

    private String taskId;
    private SearchEventAndCase searchEventAndCase = new SearchEventAndCase(
        "some-caseId",
        "decideAnApplication",
        "ia",
        "asylum"
    );

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        lenient().when(serviceAuthTokenGenerator.generate()).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

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
    void should_return_task_with_old_permissions_when_granular_permission_flag_off() throws Exception {
        String caseId = "searchForCompletableCaseId3";
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
        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
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
            get("/task/" + taskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpectAll(
            status().isOk(),
            jsonPath("$.roles").isNotEmpty(),
            jsonPath("$.roles.length()").value(1),
            jsonPath("$.roles[0].role_category").value("LEGAL_OPERATIONS"),
            jsonPath("$.roles[0].role_name").value("tribunal-caseworker"),
            jsonPath("$.roles[0].permissions[0]").value("Read"),
            jsonPath("$.roles[0].permissions[1]").value("Own"),
            jsonPath("$.roles[0].permissions[2]").value("Execute"),
            jsonPath("$.roles[0].permissions.length()").value(3)
        ).andReturn();
    }

    @Test
    void should_return_task_with_granular_permissions_when_permission_flag_on() throws Exception {
        String caseId = "searchForCompletableCaseId4";
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
        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        // Task created is IA
        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
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

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_4_GRANULAR_PERMISSION_RESPONSE,
            mockedUserInfo.getUid(),
            mockedUserInfo.getEmail()
        )).thenReturn(true);

        mockMvc.perform(
            get("/task/" + taskId + "/roles")
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
            ).andDo(print())
            .andExpectAll(
            status().isOk(),
            jsonPath("$.roles").isNotEmpty(),
            jsonPath("$.roles.length()").value(1),
            jsonPath("$.roles[0].role_category").value("LEGAL_OPERATIONS"),
            jsonPath("$.roles[0].role_name").value("tribunal-caseworker"),
            jsonPath("$.roles[0].permissions[0]").value("Read"),
            jsonPath("$.roles[0].permissions[1]").value("Own"),
            jsonPath("$.roles[0].permissions[2]").value("Execute"),
            jsonPath("$.roles[0].permissions[3]").value("Complete"),
            jsonPath("$.roles[0].permissions[4]").value("CompleteOwn"),
            jsonPath("$.roles[0].permissions[5]").value("CancelOwn"),
            jsonPath("$.roles[0].permissions[6]").value("Claim"),
            jsonPath("$.roles[0].permissions[7]").value("Unclaim"),
            jsonPath("$.roles[0].permissions[8]").value("Assign"),
            jsonPath("$.roles[0].permissions[9]").value("Unassign"),
            jsonPath("$.roles[0].permissions[10]").value("UnclaimAssign"),
            jsonPath("$.roles[0].permissions[11]").value("UnassignClaim"),
            jsonPath("$.roles[0].permissions.length()").value(12)
        ).andReturn();
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
}
