package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.config.LaunchDarklyFeatureFlagProvider;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteCaseTasksAction;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.IntegrationTestUtils;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

@SpringBootTest
@ActiveProfiles({"integration"})
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@ExtendWith(OutputCaptureExtension.class)
public class DeleteTasksControllerTest {
    @MockitoBean
    private IdamWebApi idamWebApi;
    @MockitoBean
    private CamundaServiceApi camundaServiceApi;
    @MockitoBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockitoBean
    private AuthTokenGenerator authTokenGenerator;
    @MockitoBean
    private ClientAccessControlService clientAccessControlService;
    @MockitoBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockitoBean
    private LaunchDarklyFeatureFlagProvider launchDarklyFeatureFlagProvider;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @Autowired
    MockMvc mockMvc;
    @Autowired
    IntegrationTestUtils integrationTestUtils;
    @Autowired
    private TaskResourceRepository taskResourceRepository;

    private ServiceMocks mockServices;

    @BeforeAll
    void setUp() {
        mockServices = new ServiceMocks(
                idamWebApi,
                serviceAuthorisationApi,
                camundaServiceApi,
                roleAssignmentServiceApi
        );
    }

    @AfterEach
    void tearDown() {
        taskResourceRepository.deleteAll();
    }

    @Test
    void shouldPopulateCaseDeletionTimestampWhenDeleteByCaseIdIsCalled() throws Exception {
        final String caseId = "1615817621013640";

        final String taskId1 = UUID.randomUUID().toString();
        final String taskId2 = UUID.randomUUID().toString();
        final String taskId3 = UUID.randomUUID().toString();

        insertDummyTaskInDb(taskId1, caseId, TERMINATED);
        insertDummyTaskInDb(taskId2, caseId, TERMINATED);
        insertDummyTaskInDb(taskId3, caseId, TERMINATED);

        final List<TaskResourceCaseQueryBuilder> tasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(tasks.size()).isEqualTo(3);

        assertThat(tasks.get(0).getTaskId()).isIn(taskId1, taskId2, taskId3);
        assertThat(tasks.get(1).getTaskId()).isIn(taskId1, taskId2, taskId3);
        assertThat(tasks.get(2).getTaskId()).isIn(taskId1, taskId2, taskId3);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(true);
        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        mockMvc.perform(
                post("/task/delete")
                    .content(integrationTestUtils
                                 .asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(caseId))))
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(status().isCreated()).andReturn();

        final List<TaskResourceCaseQueryBuilder> deletedTasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(deletedTasks.size()).isEqualTo(3);

        final List<TaskResource> taskResourceList = cftTaskDatabaseService.findByCaseIdOnly(caseId);
        OffsetDateTime expected = OffsetDateTime.now(ZoneOffset.UTC);
        taskResourceList.forEach(
            taskResource -> {
                assertThat(taskResource.getCaseDeletionTimestamp()).isNotNull();
                assertThat(taskResource.getCaseDeletionTimestamp())
                    .isCloseTo(expected, within(5, ChronoUnit.SECONDS));
            });
    }

    @Test
    void shouldLogErrorForNonTerminatedTasksWhenDeleteIsCalled(final CapturedOutput output) throws Exception {
        final String caseId = "1615817621013640";

        final String taskId1 = UUID.randomUUID().toString();
        final String taskId2 = UUID.randomUUID().toString();
        final String taskId3 = UUID.randomUUID().toString();

        insertDummyTaskInDb(taskId1, caseId, UNASSIGNED);
        insertDummyTaskInDb(taskId2, caseId, TERMINATED);
        insertDummyTaskInDb(taskId3, caseId, UNASSIGNED);

        final List<TaskResourceCaseQueryBuilder> tasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(tasks.size()).isEqualTo(3);

        assertThat(tasks.get(0).getTaskId()).isIn(taskId1, taskId2, taskId3);
        assertThat(tasks.get(1).getTaskId()).isIn(taskId1, taskId2, taskId3);
        assertThat(tasks.get(2).getTaskId()).isIn(taskId1, taskId2, taskId3);

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(true);
        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);

        mockMvc.perform(
                post("/task/delete")
                    .content(integrationTestUtils
                                 .asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(caseId))))
                    .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                    .contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpectAll(status().isCreated()).andReturn();

        final List<TaskResourceCaseQueryBuilder> deletedTasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);

        assertThat(deletedTasks.size()).isEqualTo(3);
        assertTrue(output.getOut().contains(String.format(
            "UNTERMINATED tasks marked for deletion: %s for caseId: %s",
            Arrays.asList(taskId1, taskId3), caseId
        )));

        final List<TaskResource> taskResourceList = cftTaskDatabaseService.findByCaseIdOnly(caseId);
        OffsetDateTime expected = OffsetDateTime.now(ZoneOffset.UTC);
        taskResourceList.forEach(
            taskResource -> {
                assertThat(taskResource.getCaseDeletionTimestamp()).isNotNull();
                assertThat(taskResource.getCaseDeletionTimestamp())
                    .isCloseTo(expected, within(5, ChronoUnit.SECONDS));
            });
    }

    @Test
    void shouldReturnBadResponseError() throws Exception {
        final String caseId = "123";

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(true);
        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(true);
        mockMvc.perform(
                        post("/task/delete")
                                .content(integrationTestUtils
                                             .asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void shouldReturnForbiddenResponseError() throws Exception {
        final String caseId = "1615817621013640";

        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(true);
        when(clientAccessControlService.hasPrivilegedAccess(SERVICE_AUTHORIZATION_TOKEN))
            .thenReturn(false);
        mockMvc.perform(
                        post("/task/delete")
                                .content(integrationTestUtils
                                             .asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden())
                .andReturn();
    }

    @Test
    void shouldReturnServiceUnavailableError() throws Exception {
        final String caseId = "123";
        when(launchDarklyFeatureFlagProvider.getBooleanValue(any(), any(), any())).thenReturn(false);

        mockMvc.perform(
                        post("/task/delete")
                                .content(integrationTestUtils
                                             .asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isServiceUnavailable())
                .andReturn();
    }

    private void insertDummyTaskInDb(final String taskId, final String caseId, final CFTTaskState cftTaskState) {
        final TaskResource taskResource = getTaskResource(taskId, caseId, cftTaskState);

        final TaskRoleResource tribunalResource = new TaskRoleResource(
                "tribunal-caseworker", true, false, false, false, true,
                true, new String[]{}, 1, false, "LegalOperations"
        );
        tribunalResource.setTaskId(taskId);
        final Set<TaskRoleResource> taskRoleResourceSet = Set.of(tribunalResource);
        taskResource.setTaskRoleResources(taskRoleResourceSet);
        cftTaskDatabaseService.saveTask(taskResource);
    }

    private static TaskResource getTaskResource(String taskId, String caseId, CFTTaskState cftTaskState) {
        final TaskResource taskResource = new TaskResource(
                taskId,
                "someTaskName",
                "someTaskType",
                cftTaskState
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setDueDateTime(OffsetDateTime.now());
        taskResource.setJurisdiction("IA");
        taskResource.setCaseTypeId("Asylum");
        taskResource.setSecurityClassification(SecurityClassification.PUBLIC);
        taskResource.setLocation("765324");
        taskResource.setLocationName("Taylor House");
        taskResource.setRegion("TestRegion");
        taskResource.setCaseId(caseId);
        return taskResource;
    }
}
