package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionEvaluatorService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.permission.PermissionRequirements;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.CftQueryService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaTask;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.HistoryVariableInstance;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.CANCELLED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.domain.entities.camunda.CamundaVariableDefinition.CFT_TASK_STATE;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

class PostTaskCancelByIdControllerTest extends SpringBootIntegrationBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/cancel";
    private static String ENDPOINT_BEING_TESTED;
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
    private AccessControlService accessControlService;
    @MockBean
    private PermissionEvaluatorService permissionEvaluatorService;
    @MockBean
    private CftQueryService cftQueryService;

    @MockBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Mock
    private RoleAssignment mockedRoleAssignment;
    @Mock
    private UserInfo mockedUserInfo;
    @Autowired
    private TaskResourceRepository taskResourceRepository;
    private ServiceMocks mockServices;
    private String taskId;

    @BeforeEach
    void setUp() {
        taskId = UUID.randomUUID().toString();
        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

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
    void should_succeed_and_return_204_and_update_cft_task_state() throws Exception {

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));
        initiateATask(taskId);

        when(permissionEvaluatorService.hasAccess(any(), any(), any()))
            .thenReturn(true);

        CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

        TaskResource task = spy(TaskResource.class);
        when(cftQueryService.getTask(anyString(), any(), any(PermissionRequirements.class)))
            .thenReturn(Optional.of(task));

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        doNothing().when(camundaServiceApi).bpmnEscalation(any(), any(), any());

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isNoContent());

        Optional<TaskResource> taskResource = taskResourceRepository.getByTaskId(taskId);

        assertTrue(taskResource.isPresent());
        assertEquals(CANCELLED, taskResource.get().getState());
    }

    @Test
    void should_succeed_and_return_204_when_task_is_already_deleted() throws Exception {

        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, singletonList(mockedRoleAssignment)));
        initiateATask(taskId);

        when(permissionEvaluatorService.hasAccess(any(), any(), any()))
            .thenReturn(true);

        CamundaTask camundaTasks = mockServices.getCamundaTask("processInstanceId", taskId);
        when(camundaServiceApi.getTask(any(), eq(taskId))).thenReturn(camundaTasks);

        when(camundaServiceApi.searchHistory(eq(IDAM_AUTHORIZATION_TOKEN), any()))
            .thenReturn(singletonList(new HistoryVariableInstance(
                "someId",
                CFT_TASK_STATE.value(),
                "some state"
            )));

        TaskResource task = spy(TaskResource.class);
        when(cftQueryService.getTask(anyString(), any(), any(PermissionRequirements.class)))
            .thenReturn(Optional.of(task));

        when(launchDarklyFeatureFlagProvider.getBooleanValue(
            FeatureFlag.RELEASE_2_ENDPOINTS_FEATURE,
            IDAM_USER_ID,
            IDAM_USER_EMAIL
        )).thenReturn(true);

        mockMvc.perform(
                post(ENDPOINT_BEING_TESTED)
                    .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(status().isNoContent());

        Optional<TaskResource> taskResource = taskResourceRepository.getByTaskId(taskId);

        assertTrue(taskResource.isPresent());
        assertEquals(CANCELLED, taskResource.get().getState());
    }

    private void initiateATask(String id) {

        TaskResource taskResource = new TaskResource(
            id,
            "taskName",
            "taskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
    }

}
