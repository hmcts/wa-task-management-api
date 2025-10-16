package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.AccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.access.entities.AccessControlResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.IdamService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.Token;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.idam.entities.UserInfo;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.response.RoleAssignmentResource;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.config.features.FeatureFlag;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.CompleteTaskRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.enums.TestRolesWithGrantType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.enums.TaskAction;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.TaskTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.COMPLETED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_OTHER_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_EMAIL;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_USER_ID;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class PostTaskCompleteByIdControllerReplicaTest extends ReplicaBaseTest {

    private static final String ENDPOINT_PATH = "/task/%s/complete";
    private static String ENDPOINT_BEING_TESTED;

    @MockitoBean
    LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;

    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;

    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @MockitoBean
    private IdamService idamService;
    @MockitoBean
    private AccessControlService accessControlService;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;

    @Mock
    private UserInfo mockedUserInfo;

    private ServiceMocks mockServices;

    TaskTestUtils taskTestUtils;

    @BeforeAll
    void init() {
        taskTestUtils = new TaskTestUtils(cftTaskDatabaseService,"replica");
    }

    @Test
    void should_update_reportable_task_values_when_complete_api_invoked() throws Exception {

        final String taskId = UUID.randomUUID().toString();

        when(clientAccessControlService.hasPrivilegedAccess(eq(SERVICE_AUTHORIZATION_TOKEN), any()))
            .thenReturn(false);

        lenient().when(launchDarklyFeatureFlagProvider.getBooleanValue(eq(FeatureFlag.WA_COMPLETION_PROCESS_UPDATE),
                                                                       anyString(), anyString())).thenReturn(false);
        when(authTokenGenerator.generate())
            .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        lenient().when(mockedUserInfo.getUid())
            .thenReturn(IDAM_USER_ID);
        lenient().when(mockedUserInfo.getEmail())
            .thenReturn(IDAM_USER_EMAIL);

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );

        mockServices.mockUserInfo();

        RoleAssignmentRequest roleAssignmentRequest = RoleAssignmentRequest.builder()
            .testRolesWithGrantType(TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC)
            .roleAssignmentAttribute(
                RoleAssignmentAttribute.builder()
                    .jurisdiction("IA")
                    .caseType("Asylum")
                    .caseId("completeCaseId1")
                    .build()
            )
            .build();

        List<RoleAssignment> roleAssignments = new ArrayList<>();

        createRoleAssignment(roleAssignments, roleAssignmentRequest);

        final RoleAssignmentResource accessControlResponse = new RoleAssignmentResource(roleAssignments);



        TaskRoleResource taskRoleResource = new TaskRoleResource(
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleName(),
            true, true, true, false, false, false,
            new String[]{}, 1, false,
            TestRolesWithGrantType.STANDARD_TRIBUNAL_CASE_WORKER_PUBLIC.getRoleCategory().name()
        );

        taskRoleResource.setComplete(true);

        when(idamService.getUserInfo(IDAM_AUTHORIZATION_TOKEN)).thenReturn(mockedUserInfo);
        when(roleAssignmentServiceApi.getRolesForUser(
            any(), any(), any()
        )).thenReturn(accessControlResponse);
        when(accessControlService.getRoles(IDAM_AUTHORIZATION_TOKEN))
            .thenReturn(new AccessControlResponse(mockedUserInfo, roleAssignments));

        lenient().when(mockedUserInfo.getUid())
            .thenReturn(IDAM_OTHER_USER_ID);
        when(idamWebApi.token(any())).thenReturn(new Token(IDAM_AUTHORIZATION_TOKEN, "scope"));
        when(serviceAuthorisationApi.serviceToken(any())).thenReturn(SERVICE_AUTHORIZATION_TOKEN);

        doNothing().when(camundaServiceApi).assignTask(any(), any(), any());
        doNothing().when(camundaServiceApi).addLocalVariablesToTask(any(), any(), any());

        taskTestUtils.insertDummyTaskInDb("IA", "Asylum", "completeCaseId1",
                                          taskId,UNASSIGNED, taskRoleResource,null, IDAM_USER_ID);

        ENDPOINT_BEING_TESTED = String.format(ENDPOINT_PATH, taskId);

        CompleteTaskRequest request = new CompleteTaskRequest(null);
        mockMvc.perform(
            post(ENDPOINT_BEING_TESTED)
                .header(AUTHORIZATION, IDAM_AUTHORIZATION_TOKEN)
                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                .contentType(APPLICATION_JSON_VALUE)
                .content(asJsonString(request))
        ).andExpectAll(
            status().isNoContent()
        );

        await()
            .pollDelay(5, SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {
                List<ReportableTaskResource> reportableTaskList2
                    = miReportingServiceForTest.findByReportingTaskId(taskId);
                assertEquals(1, reportableTaskList2.size());
            });

        Optional<TaskResource> taskResourcePostComplete = cftTaskDatabaseService.findByIdOnly(taskId);

        assertTrue(taskResourcePostComplete.isPresent());
        assertEquals(COMPLETED, taskResourcePostComplete.get().getState());
        assertEquals(IDAM_USER_ID, taskResourcePostComplete.get().getAssignee());
        assertNotNull(taskResourcePostComplete.get().getLastUpdatedTimestamp());
        assertEquals(IDAM_OTHER_USER_ID, taskResourcePostComplete.get().getLastUpdatedUser());
        assertEquals(TaskAction.COMPLETED.getValue(), taskResourcePostComplete.get().getLastUpdatedAction());

        await()
            .pollDelay(5, SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                List<TaskHistoryResource> taskHistoryResourceList
                    = miReportingServiceForTest.findByTaskId(taskId);

                assertEquals(2, taskHistoryResourceList.size());
                assertEquals("COMPLETED", taskHistoryResourceList.get(1).getState());
                assertNotNull(taskHistoryResourceList.get(1).getAssignee());
                assertNotNull(taskHistoryResourceList.get(1).getUpdatedBy());
                assertNotNull(taskHistoryResourceList.get(1).getUpdated());
                assertEquals("Complete", taskHistoryResourceList.get(1).getUpdateAction());
                assertNotNull(taskHistoryResourceList.get(1).getCreated());
                assertNotNull(taskHistoryResourceList.get(1).getDueDateTime());
            });

        await()
            .pollDelay(5, SECONDS)
            .atMost(30, SECONDS)
            .untilAsserted(() -> {

                List<ReportableTaskResource> reportableTaskList
                    = miReportingServiceForTest.findByReportingTaskId(taskId);

                assertEquals(1, reportableTaskList.size());
                assertEquals("COMPLETED", reportableTaskList.get(0).getState());
                assertNotNull(reportableTaskList.get(0).getAssignee());
                assertNotNull(reportableTaskList.get(0).getUpdatedBy());
                assertNotNull(reportableTaskList.get(0).getUpdated());
                assertEquals("Complete", reportableTaskList.get(0).getUpdateAction());
                assertNotNull(reportableTaskList.get(0).getCompletedDate());
                assertNotNull(reportableTaskList.get(0).getCompletedDateTime());
                assertNotNull(reportableTaskList.get(0).getCreatedDate());
                assertNotNull(reportableTaskList.get(0).getDueDate());
                assertNotNull(reportableTaskList.get(0).getLastUpdatedDate());
                assertEquals("COMPLETED", reportableTaskList.get(0).getFinalStateLabel());
                assertNotNull(reportableTaskList.get(0).getFirstAssignedDate());
                assertNotNull(reportableTaskList.get(0).getFirstAssignedDateTime());
                Assertions.assertNull(reportableTaskList.get(0).getWaitTimeDays());
                Assertions.assertNull(reportableTaskList.get(0).getWaitTime());
                assertNotNull(reportableTaskList.get(0).getHandlingTimeDays());
                assertNotNull(reportableTaskList.get(0).getHandlingTime());
                assertNotNull(reportableTaskList.get(0).getProcessingTimeDays());
                assertNotNull(reportableTaskList.get(0).getProcessingTime());
                assertEquals("Yes", reportableTaskList.get(0).getIsWithinSla());
                assertEquals(0, reportableTaskList.get(0).getNumberOfReassignments());
                assertEquals(-1, reportableTaskList.get(0).getDueDateToCompletedDiffDays());
                assertNotNull(reportableTaskList.get(0).getDueDateToCompletedDiffTime());
            });

    }

}
