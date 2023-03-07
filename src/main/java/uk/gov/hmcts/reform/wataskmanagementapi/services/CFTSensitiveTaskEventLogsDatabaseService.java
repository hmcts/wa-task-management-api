package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.microsoft.applicationinsights.telemetry.TelemetryContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.wataskmanagementapi.auth.role.entities.RoleAssignment;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.v2.enums.ErrorMessages;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@SuppressWarnings("PMD.DoNotUseThreads")
public class CFTSensitiveTaskEventLogsDatabaseService {
    private final SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;
    private final CFTTaskDatabaseService cftTaskDatabaseService;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private TelemetryContext telemetryContext;


    public CFTSensitiveTaskEventLogsDatabaseService(SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository,
                                                    CFTTaskDatabaseService cftTaskDatabaseService) {
        this.sensitiveTaskEventLogsRepository = sensitiveTaskEventLogsRepository;
        this.cftTaskDatabaseService = cftTaskDatabaseService;
    }

    public SensitiveTaskEventLog saveSensitiveTaskEventLog(SensitiveTaskEventLog sensitiveTaskEventLog) {
        return sensitiveTaskEventLogsRepository.save(sensitiveTaskEventLog);
    }

    public void processSensitiveTaskEventLog(String taskId,
                                             List<RoleAssignment> roleAssignments,
                                             ErrorMessages customErrorMessage) {
        telemetryContext = new TelemetryContext();
        executorService.execute(() -> {
            Optional<TaskResource> taskResource = cftTaskDatabaseService.findByIdOnly(taskId);
            if (taskResource.isPresent()) {
                taskResource.get().getTaskRoleResources();

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
                saveSensitiveTaskEventLog(sensitiveTaskEventLog);

            }
        });
    }

    public int cleanUpSensitiveLogs(LocalDateTime timeStamp) {
        return sensitiveTaskEventLogsRepository.cleanUpSensitiveLogs(timeStamp);
    }
}
