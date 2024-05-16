package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.ExecuteReconfigureTaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(value = {MockitoExtension.class, OutputCaptureExtension.class})
class ExecuteTaskReconfigurationFailureServiceTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;
    @InjectMocks
    private ExecuteTaskReconfigurationFailureService taskReconfigurationFailureService;

    @Test
    void should_get_reconfiguration_fail_log(CapturedOutput output) {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();
        List<TaskResource> taskResources = taskResourcesToReconfigure(OffsetDateTime.now());

        when(cftTaskDatabaseService
                 .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(anyList(), any())
        ).thenReturn(taskResources);

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES)
                .runId("")
                .maxTimeLimit(2)
                .build(), taskFilters
        );

        Map<String, Object> resourceMap = taskReconfigurationFailureService.performOperation(request).getResponseMap();
        int actualTasks = (int) resourceMap.get("successfulTaskResources");
        assertEquals(actualTasks, taskResources.size());
    }

    @Test
    void should_get_no_tasks_on_reconfiguration_fail_log() {

        List<TaskFilter<?>> taskFilters = createReconfigureTaskFilters();

        when(cftTaskDatabaseService
                 .getActiveTasksAndReconfigureRequestTimeIsLessThanRetry(anyList(), any())
        ).thenReturn(Collections.emptyList());

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.EXECUTE_RECONFIGURE_FAILURES)
                .runId("")
                .maxTimeLimit(2)
                .build(), taskFilters
        );

        Map<String, Object> resourceMap = taskReconfigurationFailureService.performOperation(request).getResponseMap();
        int taskResourcesReconfigured = (int) resourceMap.get("successfulTaskResources");
        assertEquals(0, taskResourcesReconfigured);
    }

    private List<TaskFilter<?>> createReconfigureTaskFilters() {
        ExecuteReconfigureTaskFilter filter = new ExecuteReconfigureTaskFilter(
            "reconfigure_request_time", OffsetDateTime.now().minus(Duration.ofDays(10)), TaskFilterOperator.AFTER);
        return List.of(filter);
    }

    private List<TaskResource> taskResourcesToReconfigure(OffsetDateTime reconfigureTime) {
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
        if (Objects.nonNull(reconfigureTime)) {
            taskResource1.setLastReconfigurationTime(reconfigureTime.minusDays(2));
            taskResource2.setLastReconfigurationTime(reconfigureTime.minusDays(2));
        }
        return List.of(taskResource1, taskResource2);
    }

}
