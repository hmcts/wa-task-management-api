package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.MarkTaskToReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.response.TaskOperationResponse;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class TaskOperationServiceTest {


    @Mock
    MarkTaskReconfigurationService markTaskReconfigurationService;
    @Mock
    ExecuteTaskReconfigurationService executeTaskReconfigurationService;
    @Mock
    ExecuteTaskReconfigurationFailureService executeTaskReconfigurationFailureService;

    @Mock
    CleanUpSensitiveLogsService cleanUpSensitiveLogsService;
    @Mock
    UpdateSearchIndexService updateSearchIndexService;

    TaskOperationService taskOperationService;
    List<TaskResource> tasks;
    TaskOperationResponse taskOperationResponse;

    @BeforeEach
    public void setUp() {
        taskOperationService = new TaskOperationService(
            List.of(markTaskReconfigurationService,
                    executeTaskReconfigurationService,
                    updateSearchIndexService,
                    executeTaskReconfigurationFailureService,
                    cleanUpSensitiveLogsService)
        );

        tasks = taskResources(false);
        taskOperationResponse = new TaskOperationResponse(Map.of("successfulTaskResources", tasks.size()));

        lenient().when(markTaskReconfigurationService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.MARK_TO_RECONFIGURE))))
            .thenReturn(taskOperationResponse);

        lenient().when(executeTaskReconfigurationService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.EXECUTE_RECONFIGURE))))
            .thenReturn(taskOperationResponse);

        lenient().when(updateSearchIndexService.performOperation(
                argThat(argument -> argument.getOperation().getType().equals(TaskOperationType.UPDATE_SEARCH_INDEX))))
            .thenReturn(taskOperationResponse);
        lenient().when(executeTaskReconfigurationFailureService.performOperation(
                argThat(argument -> argument.getOperation().getType()
                    .equals(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES))))
            .thenReturn(taskOperationResponse);
        lenient().when(cleanUpSensitiveLogsService.performOperation(
                argThat(argument -> argument.getOperation().getType()
                    .equals(TaskOperationType.CLEANUP_SENSITIVE_LOG_ENTRIES))))
            .thenReturn(taskOperationResponse);
    }

    @Test
    void should_perform_operation_for_all_operation_type() {
        Arrays.asList(TaskOperationType.values()).forEach(operationType -> {
            Map<String, Object> input = taskOperationService.performOperation(
                taskOperationRequest(operationType)).getResponseMap();
            if (input.containsKey("successfulTaskResources")) {
                assertEquals(2, (int) input.get("successfulTaskResources"));
            }
            if (input.containsKey("replicationCheckedTaskIds")) {
                assertEquals(2, (int) input.get("replicationCheckedTaskIds"));
            }
        });
    }

    private List<TaskResource> taskResources(boolean index) {
        TaskResource taskResource1 = new TaskResource(
            "1234",
            "someTaskName",
            "someTaskType",
            CFTTaskState.UNASSIGNED,
            "someCaseId"
        );
        TaskResource taskResource2 = new TaskResource(
            "4567",
            "someTaskName",
            "someTaskType",
            CFTTaskState.ASSIGNED,
            "someCaseId"
        );
        taskResource1.setReconfigureRequestTime(OffsetDateTime.now());
        taskResource1.setIndexed(index);
        taskResource2.setReconfigureRequestTime(OffsetDateTime.now());
        taskResource2.setIndexed(index);
        return List.of(taskResource1, taskResource2);
    }

    private TaskOperationRequest taskOperationRequest(TaskOperationType type) {
        TaskOperation operation = TaskOperation.builder()
            .type(type)
            .runId("run_id1")
            .maxTimeLimit(2)
            .retryWindowHours(120)
            .build();
        return new TaskOperationRequest(operation, taskFilters());
    }

    private List<TaskFilter<?>> taskFilters() {
        TaskFilter<List<String>> filter = new MarkTaskToReconfigureTaskFilter(
            "case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }

}
