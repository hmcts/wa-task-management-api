package uk.gov.hmcts.reform.wataskmanagementapi.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.generators.AuthTokenGenerator;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.restrict.ClientAccessControlService;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.query.TaskResourceCaseQueryBuilder;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteCaseTasksAction;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.DeleteTasksRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.domain.camunda.SecurityClassification;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.TERMINATED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.config.SecurityConfiguration.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.IDAM_AUTHORIZATION_TOKEN;
import static uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks.SERVICE_AUTHORIZATION_TOKEN;

public class DeleteTasksControllerTest extends SpringBootIntegrationBaseTest {
    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    private AuthTokenGenerator authTokenGenerator;
    @MockBean
    private ClientAccessControlService clientAccessControlService;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @Autowired
    private CFTTaskDatabaseService cftTaskDatabaseService;

    private ServiceMocks mockServices;

    @BeforeEach
    void setUp() {
        when(authTokenGenerator.generate())
                .thenReturn(IDAM_AUTHORIZATION_TOKEN);
        mockServices = new ServiceMocks(
                idamWebApi,
                serviceAuthorisationApi,
                camundaServiceApi,
                roleAssignmentServiceApi
        );
    }

    @Test
    void shouldDeleteTasksByCaseId() throws Exception {
        final String caseId = "1615817621013640";

        final String taskId1 = UUID.randomUUID().toString();
        final String taskId2 = UUID.randomUUID().toString();
        final String taskId3 = UUID.randomUUID().toString();

        insertDummyTaskInDb(taskId1, caseId, UNASSIGNED);
        insertDummyTaskInDb(taskId2, caseId, TERMINATED);
        insertDummyTaskInDb(taskId3, caseId, UNASSIGNED);

        final List<TaskResourceCaseQueryBuilder> tasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);
        assertThat(tasks.get(0).getTaskId()).isEqualTo(taskId1);
        assertThat(tasks.get(1).getTaskId()).isEqualTo(taskId2);
        assertThat(tasks.get(2).getTaskId()).isEqualTo(taskId3);


        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);

        mockMvc.perform(
                        post("/task/delete")
                                .content(asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(
                                        caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpectAll(status().isCreated()).andReturn();

        final List<TaskResourceCaseQueryBuilder> deletedTasks = cftTaskDatabaseService.findByTaskIdsByCaseId(caseId);
        assertThat(deletedTasks.size()).isEqualTo(0);
    }

    @Test
    void shouldReturnBadResponseError() throws Exception {
        final String caseId = "123";
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(true);
        mockMvc.perform(
                        post("/task/delete")
                                .content(asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(
                                        caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    void shouldReturnForbiddenResponseError() throws Exception {
        final String caseId = "1615817621013640";
        when(clientAccessControlService.hasExclusiveAccess(SERVICE_AUTHORIZATION_TOKEN))
                .thenReturn(false);
        mockMvc.perform(
                        post("/task/delete")
                                .content(asJsonString(new DeleteTasksRequest(new DeleteCaseTasksAction(
                                        caseId))))
                                .header(SERVICE_AUTHORIZATION, SERVICE_AUTHORIZATION_TOKEN)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isForbidden())
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