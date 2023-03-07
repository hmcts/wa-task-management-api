package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.CleanupSensitiveLogsTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationName.CLEANUP_SENSITIVE_LOG_ENTRIES;

@Slf4j
@Component
@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class CleanUpSensitiveLogsService implements TaskOperationService {

    private final CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    public CleanUpSensitiveLogsService(CFTSensitiveTaskEventLogsDatabaseService
                                           cftSensitiveTaskEventLogsDatabaseService) {
        this.cftSensitiveTaskEventLogsDatabaseService = cftSensitiveTaskEventLogsDatabaseService;
    }

    @Override
    @Transactional
    public TaskOperationResponse performOperation(TaskOperationRequest taskOperationRequest) {
        if (CLEANUP_SENSITIVE_LOG_ENTRIES.equals(taskOperationRequest.getOperation().getName())) {
            return cleanUpSensitiveLogs(taskOperationRequest);
        }
        return new TaskOperationResponse();
    }

    private TaskOperationResponse cleanUpSensitiveLogs(TaskOperationRequest request) {
        log.debug("{} request: {}", CLEANUP_SENSITIVE_LOG_ENTRIES.name(), request);

        LocalDateTime cleanUpStartDate = getCleanUpStartDate(request.getTaskFilter());
        Objects.requireNonNull(cleanUpStartDate);

        int deletedRows = cftSensitiveTaskEventLogsDatabaseService.cleanUpSensitiveLogs(cleanUpStartDate);

        return new TaskOperationResponse(Map.of("deletedRows", deletedRows));
    }

    private LocalDateTime getCleanUpStartDate(List<TaskFilter<?>> taskFilters) {

        return taskFilters.stream()
            .filter(filter -> filter.getKey().equalsIgnoreCase("clean_up_start_date"))
            .findFirst()
            .map(filter -> ((CleanupSensitiveLogsTaskFilter) filter).getValues().toLocalDateTime())
            .orElse(null);
    }

}
