package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.CleanupSensitiveLogsTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTSensitiveTaskEventLogsDatabaseService;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class CleanUpSensitiveLogsServiceTest {

    private static final String KEY = "clean_up_start_date";

    @Mock
    private CFTSensitiveTaskEventLogsDatabaseService cftSensitiveTaskEventLogsDatabaseService;

    @Mock
    private SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @InjectMocks
    private CleanUpSensitiveLogsService cleanUpSensitiveLogsService;

    @Test
    void should_return_empty_response_when_invalid_operation_name_provided() {
        OffsetDateTime timestamp = OffsetDateTime.now();

        List<TaskFilter<?>> taskFilters = createTaskFilters(KEY, timestamp);
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE)
                .runId("123")
                .build(),
            taskFilters
        );

        TaskOperationResponse taskOperationResponse = cleanUpSensitiveLogsService
            .performOperation(request);

        assertNotNull(taskOperationResponse);

        assertNull(taskOperationResponse.getResponseMap());

    }

    @Test
    void should_clean_up_sensitive_logs() {
        OffsetDateTime timestamp = OffsetDateTime.now();

        List<TaskFilter<?>> taskFilters = createTaskFilters(KEY, timestamp);
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.CLEANUP_SENSITIVE_LOG_ENTRIES)
                .runId("")
                .build(),
            taskFilters
        );

        TaskOperationResponse taskOperationResponse = cleanUpSensitiveLogsService
            .performOperation(request);

        assertNotNull(taskOperationResponse);

        int deletedRows = (int) taskOperationResponse.getResponseMap()
            .get("deletedRows");

        assertEquals(0, deletedRows);

    }

    @Test
    void should_log_exception_when_clean_up_sensitive_logs_fails() {
        OffsetDateTime timestamp = OffsetDateTime.now();

        List<TaskFilter<?>> taskFilters = createTaskFilters(KEY, timestamp);
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.CLEANUP_SENSITIVE_LOG_ENTRIES)
                .runId("")
                .build(),
            taskFilters
        );

        doThrow(new IllegalArgumentException("cleanup exception"))
            .when(cftSensitiveTaskEventLogsDatabaseService).cleanUpSensitiveLogs(any(LocalDateTime.class));

        TaskOperationResponse taskOperationResponse = cleanUpSensitiveLogsService.performOperation(request);

        assertNotNull(taskOperationResponse);

        String exceptionMessage = (String) taskOperationResponse.getResponseMap().get("exception");

        assertEquals("cleanup exception", exceptionMessage);
    }

    @Test
    void should_throw_exception_when_invalid_key_provided() {
        OffsetDateTime timestamp = OffsetDateTime.now();

        List<TaskFilter<?>> taskFilters = createTaskFilters("INVALID_KEY", timestamp);
        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.CLEANUP_SENSITIVE_LOG_ENTRIES)
                .runId("")
                .build(),
            taskFilters
        );

        assertThrows(
            NullPointerException.class,
            () -> cleanUpSensitiveLogsService.performOperation(request)
        );

    }

    private List<TaskFilter<?>> createTaskFilters(String key, OffsetDateTime timestamp) {
        CleanupSensitiveLogsTaskFilter filter = new CleanupSensitiveLogsTaskFilter(
            key, timestamp, TaskFilterOperator.BEFORE);
        return List.of(filter);
    }
}

