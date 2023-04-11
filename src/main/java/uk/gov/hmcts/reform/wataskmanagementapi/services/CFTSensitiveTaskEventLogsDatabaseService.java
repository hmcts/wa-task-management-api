package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

@Slf4j
@Service
@SuppressWarnings("PMD.DoNotUseThreads")
public class CFTSensitiveTaskEventLogsDatabaseService {
    private final SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;
    private final CFTTaskDatabaseService cftTaskDatabaseService;

    private final ExecutorService sensitiveTaskEventLogsExecutorService;


    public CFTSensitiveTaskEventLogsDatabaseService(SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository,
                                                    CFTTaskDatabaseService cftTaskDatabaseService,
                                                    ExecutorService sensitiveTaskEventLogsExecutorService) {
        this.sensitiveTaskEventLogsRepository = sensitiveTaskEventLogsRepository;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
        this.sensitiveTaskEventLogsExecutorService = sensitiveTaskEventLogsExecutorService;
    }

    private SensitiveTaskEventLog saveSensitiveTaskEventLog(SensitiveTaskEventLog sensitiveTaskEventLog) {
        return sensitiveTaskEventLogsRepository.save(sensitiveTaskEventLog);
    }

    public void processSensitiveTaskEventLog(String taskId,
                                             List<RoleAssignment> roleAssignments,
                                             ErrorMessages customErrorMessage) {
        TelemetryContext telemetryContext = new TelemetryContext();
        Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);
        if (taskResource.isPresent()) {
            log.info("TaskRoles for taskId {} is {}", taskId, taskResource.get().getTaskRoleResources());
            SensitiveTaskEventLog sensitiveTaskEventLog = new SensitiveTaskEventLog(
                UUID.randomUUID().toString(),
                telemetryContext.getOperation().getId(),
                "",
                taskId,
                taskResource.get().getCaseId(),
                customErrorMessage.getDetail(),
                List.of(taskResource.get()),
                roleAssignments,
                ZonedDateTime.now().toOffsetDateTime().plusDays(90),
                ZonedDateTime.now().toOffsetDateTime()
            );

            sensitiveTaskEventLogsExecutorService.execute(() -> {
                saveSensitiveTaskEventLog(sensitiveTaskEventLog);
            });

        }

    }

    @PreDestroy
    public void cleanup() {
        log.info("Shutting down sensitiveTaskEventLogs executor");
        sensitiveTaskEventLogsExecutorService.shutdown();
        try {
            // Wait a while for existing job to complete
            if (!sensitiveTaskEventLogsExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                sensitiveTaskEventLogsExecutorService.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for existing job being cancelled
                if (!sensitiveTaskEventLogsExecutorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            sensitiveTaskEventLogsExecutorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
        log.info("Shut down sensitiveTaskEventLogs events executor");
    }
}
