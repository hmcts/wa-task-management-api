package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

public class CFTSensitiveTaskEventLogsDatabaseServiceTest  extends SpringBootIntegrationBaseTest {

    @Autowired
    SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @MockBean
    private IdamWebApi idamWebApi;
    @MockBean
    private ServiceAuthorisationApi serviceAuthorisationApi;
    @MockBean
    private CamundaServiceApi camundaServiceApi;
    @MockBean
    private RoleAssignmentServiceApi roleAssignmentServiceApi;
    @MockBean
    CFTTaskDatabaseService cftTaskDatabaseService;

    private ServiceMocks mockServices;

    CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @BeforeEach
    void setUp() {
        cftSensitiveTaskEventLogsDatabaseService =
            new CFTSensitiveTaskEventLogsDatabaseService(sensitiveTaskEventLogsRepository, cftTaskDatabaseService);

        mockServices = new ServiceMocks(
            idamWebApi,
            serviceAuthorisationApi,
            camundaServiceApi,
            roleAssignmentServiceApi
        );
    }

    @Test
    void should_succeed_and_save_sensitive_task_event_log() {
        String taskId = UUID.randomUUID().toString();
        String caseId = "Some caseId";
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setCaseId(caseId);

        final List<String> roleNames = singletonList("tribunal-caseworker");

        List<RoleAssignment> roleAssignments =
            mockServices.createTestRoleAssignments(roleNames);

        SensitiveTaskEventLog sensitiveTaskEventLog = new SensitiveTaskEventLog(
            UUID.randomUUID().toString(),
            null,
            null,
            taskId,
            taskResource.getCaseId(),
            "Some Message",
            List.of(taskResource),
            roleAssignments,
            ZonedDateTime.now().toOffsetDateTime().plusDays(90),
            ZonedDateTime.now().toOffsetDateTime()
        );

        SensitiveTaskEventLog updatedSensitiveTaskEventLog =
            cftSensitiveTaskEventLogsDatabaseService.saveSensitiveTaskEventLog(sensitiveTaskEventLog);
        assertNotNull(updatedSensitiveTaskEventLog);
        assertEquals(taskId, updatedSensitiveTaskEventLog.getTaskId());
        assertEquals("Some Message", updatedSensitiveTaskEventLog.getMessage());
        assertEquals(caseId, updatedSensitiveTaskEventLog.getCaseId());
    }
}
