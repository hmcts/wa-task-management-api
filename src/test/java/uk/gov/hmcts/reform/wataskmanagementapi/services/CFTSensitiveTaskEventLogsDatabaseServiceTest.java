package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

@ExtendWith(MockitoExtension.class)
class CFTSensitiveTaskEventLogsDatabaseServiceTest {

    @Mock
    ExecutorService sensitiveTaskEventLogsExecutorService;

    @Mock
    private SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @Mock
    private RoleAssignment roleAssignments;

    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @BeforeEach
    void setUp() {
        cftSensitiveTaskEventLogsDatabaseService =
            new CFTSensitiveTaskEventLogsDatabaseService(
                sensitiveTaskEventLogsRepository,
                cftTaskDatabaseService,
                sensitiveTaskEventLogsExecutorService
            );
    }

    @Test
    void should_process_sensitive_task_event_log() {
        String taskId = "someTaskId";
        String caseId = "someCaseId";
        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNCONFIGURED,
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setCaseId(caseId);

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
            taskId,
            List.of(roleAssignments),
            ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
        );

        ExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.execute(() -> {
            verify(sensitiveTaskEventLogsRepository, times(1)).save(any(SensitiveTaskEventLog.class));
        });
    }

    @Test
    void should_process_and_log_error_when_db_exception_happens() {
        String taskId = "someTaskId";
        doThrow(new RuntimeException("some unexpected error"))
            .when(sensitiveTaskEventLogsRepository).save(any(SensitiveTaskEventLog.class));

        cftSensitiveTaskEventLogsDatabaseService.processSensitiveTaskEventLog(
            taskId,
            List.of(roleAssignments),
            ErrorMessages.ROLE_ASSIGNMENT_VERIFICATIONS_FAILED_ASSIGNEE
        );
        assertThatThrownBy(() -> cftSensitiveTaskEventLogsDatabaseService.saveSensitiveTaskEventLog(any(
            SensitiveTaskEventLog.class)))
            .isInstanceOf(RuntimeException.class);
    }
}
