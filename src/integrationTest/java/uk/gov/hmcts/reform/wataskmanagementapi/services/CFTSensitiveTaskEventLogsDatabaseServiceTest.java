package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.CamundaServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.IdamWebApi;
import uk.gov.hmcts.reform.wataskmanagementapi.clients.RoleAssignmentServiceApi;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ServiceMocks;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

public class CFTSensitiveTaskEventLogsDatabaseServiceTest  extends SpringBootIntegrationBaseTest {

    @MockBean
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
    void should_process_and_save_sensitive_task_event_log() {
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

        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(java.util.Optional.of(taskResource));

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(taskId,
            roleAssignments, ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE);

        ExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.execute(() -> {
            verify(sensitiveTaskEventLogsRepository, times(1)).save(any(SensitiveTaskEventLog.class));
        });
    }

    @Test
    void should_process_and_log_error_when_db_exception_happens() {
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

        when(cftTaskDatabaseService.findByIdOnly(taskId)).thenReturn(java.util.Optional.of(taskResource));

        doThrow(new RuntimeException("some unexpected error"))
            .when(sensitiveTaskEventLogsRepository).save(any(SensitiveTaskEventLog.class));

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(taskId,
            roleAssignments, ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE);

        ExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.execute(() -> {
            assertThatThrownBy(() ->
                cftSensitiveTaskEventLogsDatabaseService.saveSensitiveTaskEventLog(any(SensitiveTaskEventLog.class)))
                .isInstanceOf(RuntimeException.class);
        });
    }
}
