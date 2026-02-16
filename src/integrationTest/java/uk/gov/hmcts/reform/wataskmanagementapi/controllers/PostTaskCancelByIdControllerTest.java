package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentAttribute;
import uk.gov.hmcts.reform.wataskmanagementapi.RoleAssignmentHelper.RoleAssignmentRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CamundaService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction.CANCEL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_OTHER_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostTaskCancelByIdControllerTest {

    private static final String ENDPOINT_PATH = "/task/%s/cancel";
    private static String ENDPOINT_BEING_TESTED;
    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private CamundaService camundaService;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockitoBean
    private AccessControlService accessControlService;
    @Mock
    private UserInfo mockedUserInfo;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    protected MockMvc mockMvc;
    RoleAssignmentHelper roleAssignmentHelper = new RoleAssignmentHelper();
    private ServiceMocks mockServices;
    private String taskId;

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
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);
    }


    @Test
    void should_succeed_and_return_204_and_update_cft_task_state() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("cancelCaseSuccessId1")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, true,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        insertDummyTaskInDb(taskId, tribunalResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isNoContent());

        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertTrue(taskResource.isPresent());
        assertEquals(CANCELLED, taskResource.get().getState());
        assertNotNull(taskResource.get().getLastUpdatedTimestamp());
        assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
        assertEquals(CANCEL.getValue(), taskResource.get().getLastUpdatedAction());
    }

    @Test
    void should_not_cancel_and_return_403_wih_no_cancel_permission() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("cancelCaseSuccessId1")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, false,
            true, new String[]{}, 1, false, "LegalOperations",
            taskId, OffsetDateTime.now(), false, false, false, true,
            false, false, false, false, false, false
        );
        insertDummyTaskInDb(taskId, tribunalResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                jsonPath("$.title").value("Role Assignment Verification"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Role Assignment Verification: "
                        + "The request failed the Role Assignment checks performed.")
            );

    }

    @ParameterizedTest
    @CsvSource(value = {
        "nullAssignee",
        "notOwnAssignee"
    })
    void should_not_cancel_and_return_403_wih_cancelown_granular_permission_and_assignee_not_own(
        String assigneeType) throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("cancelCaseSuccessId1")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, false,
            true, new String[]{}, 1, false, "LegalOperations",
            taskId, OffsetDateTime.now(), false, false, true, true,
            false, false, false, false, false, false
        );
        if (assigneeType.equals("nullAssignee")) {
            insertDummyTaskInDb(taskId, tribunalResource);
        } else {
            insertAssignedDummyTaskInDb(taskId, tribunalResource, IDAM_OTHER_USER_ID);
        }

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
                status().is4xxClientError(),
                content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
                jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
                jsonPath("$.title").value("Role Assignment Verification"),
                jsonPath("$.status").value(403),
                jsonPath("$.detail").value(
                    "Role Assignment Verification: "
                        + "The request failed the Role Assignment checks performed.")
            );

    }

    @Test
    void should_not_cancel_and_return_403_wih_no_cancelown_granular_permission() throws Exception {

        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("cancelCaseSuccessId1")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, false,
            true, new String[]{}, 1, false, "LegalOperations",
            taskId, OffsetDateTime.now(), false, false, false, true,
            false, false, false, false, false, false
        );
        insertDummyTaskInDb(taskId, tribunalResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        CamundaTask camundaTask = mockServices.getCamundaTask("processInstanceId", taskId);
        when(camundaService.getUnmappedCamundaTask(taskId)).thenReturn(camundaTask);

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpectAll(
            status().is4xxClientError(),
            content().contentType(APPLICATION_PROBLEM_JSON_VALUE),
            jsonPath("$.type").value("https://github.com/hmcts/wa-task-management-api/problem/role-assignment-verification-failure"),
            jsonPath("$.title").value("Role Assignment Verification"),
            jsonPath("$.status").value(403),
            jsonPath("$.detail").value(
                "Role Assignment Verification: "
                    + "The request failed the Role Assignment checks performed.")
            );

    }

    @Test
    void should_succeed_and_return_204_when_task_is_already_deleted() throws Exception {
        mockServices.mockUserInfo();
        List<RoleAssignment> roleAssignments = new ArrayList<>();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("cancelCaseSuccessId1")
                    .build()
            )
            .build();

        roleAssignmentHelper.createRoleAssignment(roleAssignments, roleAssignmentRequest);
        RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);
        TaskRoleResource tribunalResource = new TaskRoleResource(
            "tribunal-caseworker", true, false, false, false, true,
            true, new String[]{}, 1, false, "LegalOperations"
        );
        insertDummyTaskInDb(taskId, tribunalResource);

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        when(camundaServiceApi.searchHistory(eq(IDAM_AUTHORIZATION_TOKEN), any()))
            .thenReturn(singletonList(new HistoryVariableInstance(
                "someId",
                CFT_TASK_STATE.value(),
                "some state"
            )));

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isNoContent());

        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);

        assertTrue(taskResource.isPresent());
        assertEquals(CANCELLED, taskResource.get().getState());
        assertNotNull(taskResource.get().getLastUpdatedTimestamp());
        assertEquals(IDAM_USER_ID, taskResource.get().getLastUpdatedUser());
        assertEquals(CANCEL.getValue(), taskResource.get().getLastUpdatedAction());
    }

    private void insertDummyTaskInDb(String taskId, TaskRoleResource taskRoleResource) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED
        );
        insertTask(taskId, taskRoleResource, taskResource);
    }

    private void insertTask(String taskId, TaskRoleResource taskRoleResource, TaskResource taskResource) {
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction("IA");
        taskResource.setCaseTypeId("Asylum");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId("cancelCaseSuccessId1");

        taskRoleResource.setTaskId(taskId);
        Set<TaskRoleResource> taskRoleResourceSet = Set.of(taskRoleResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private void insertAssignedDummyTaskInDb(String taskId, TaskRoleResource taskRoleResource, String userId) {
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            ASSIGNED,
            "cancelCaseSuccessId1",
            userId
        );
        insertTask(taskId, taskRoleResource, taskResource);
    }
}
