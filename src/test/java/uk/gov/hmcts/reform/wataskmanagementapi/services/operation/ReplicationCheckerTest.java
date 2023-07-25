package uk.gov.hmcts.reform.wataskmanagementapi.services.operation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.CFTTaskDatabaseService;
import uk.gov.hmcts.reform.wataskmanagementapi.services.MIReportingService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReplicationCheckerTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @Mock
    private MIReportingService miReportingService;

    @InjectMocks
    private ReplicationChecker replicationChecker;

    private final TaskOperationRequest request = new TaskOperationRequest(
        TaskOperation.builder()
            .type(TaskOperationType.PERFORM_REPLICATION_CHECK).build(),
        List.of()
    );

    @Test
    void should_process_replication_checker_operation() {
        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(0, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());
        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();
    }

    @Test
    void should_process_replication_checker_operation_for_empty_list() {
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of());

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(0, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());
        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();
    }

    @Test
    void should_process_replication_checker_operation_for_found_history() {
        TaskResource resource = new TaskResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, times(1)).findByTaskIdOrderByLatestUpdate("1");
                return true;
            });

    }

    @Test
    void should_process_replication_checker_operation_for_not_found_history_timestamp_mismatch() {
        TaskResource resource = new TaskResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:30.345875+01:00"), "claim", "someuser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(1, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, atLeast(2)).findByTaskIdOrderByLatestUpdate("1");
                return true;
            });

    }

    @Test
    void should_process_replication_checker_operation_for_not_found_history_action_mismatch() {
        TaskResource resource = new TaskResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "assign", "someuser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(1, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, atLeast(2)).findByTaskIdOrderByLatestUpdate("1");
                return true;
            });

    }

    @Test
    void should_process_replication_checker_operation_for_not_found_history_user_mismatch() {
        TaskResource resource = new TaskResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "seconduser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(1, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, atLeast(2)).findByTaskIdOrderByLatestUpdate("1");
                return true;
            });

    }

    @Test
    void should_process_replication_checker_operation_for_found_history_multiple_tasks() {
        TaskResource resource = new TaskResource("1", OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"),
            "claim", "someuser");
        TaskResource resource1 = new TaskResource("2", OffsetDateTime.parse("2021-05-09T20:15:30.345875+01:00"),
            "assign", "seconduser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history1 = new TaskHistoryResource("2",
            OffsetDateTime.parse("2021-05-09T20:15:30.345875+01:00"), "assign", "seconduser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource, resource1));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("2")).thenReturn(List.of(history1));

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(2, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, times(1)).findByTaskIdOrderByLatestUpdate("1");
                verify(miReportingService, times(1)).findByTaskIdOrderByLatestUpdate("2");
                return true;
            });

    }

    @Test
    void should_process_replication_checker_operation_for_not_found_history_multiple_tasks() {
        TaskResource resource = new TaskResource("1", OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"),
            "claim", "someuser");
        TaskResource resource1 = new TaskResource("2", OffsetDateTime.parse("2021-05-09T20:15:30.345875+01:00"),
            "assign", "seconduser");
        TaskResource resource2 = new TaskResource("3", OffsetDateTime.parse("2021-05-09T20:15:20.345875+01:00"),
            "assign", "seconduser");
        TaskHistoryResource history = new TaskHistoryResource("1",
            OffsetDateTime.parse("2021-05-09T20:15:50.345875+01:00"), "claim", "someuser");
        TaskHistoryResource history1 = new TaskHistoryResource("2",
            OffsetDateTime.parse("2021-05-09T20:15:30.345875+01:00"), "assign", "seconduser");
        when(cftTaskDatabaseService.findLastFiveUpdatedTasks()).thenReturn(List.of(resource, resource1, resource2));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("1")).thenReturn(List.of(history));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("2")).thenReturn(List.of(history1));
        when(miReportingService.findByTaskIdOrderByLatestUpdate("3")).thenReturn(List.of());

        Map<String, Object> resourceMap = replicationChecker.performOperation(request).getResponseMap();
        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(3, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(1, notReplicated.size());

        verify(cftTaskDatabaseService, times(1)).findLastFiveUpdatedTasks();

        await()
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(() -> {
                verify(miReportingService, times(1)).findByTaskIdOrderByLatestUpdate("1");
                verify(miReportingService, times(1)).findByTaskIdOrderByLatestUpdate("2");
                verify(miReportingService, atLeast(2)).findByTaskIdOrderByLatestUpdate("3");
                return true;
            });
    }

}
