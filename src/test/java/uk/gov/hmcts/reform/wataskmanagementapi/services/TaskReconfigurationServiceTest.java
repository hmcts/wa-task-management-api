package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskFilter;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskFilterOperator;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskReconfigurationServiceTest {

    @Mock
    private CFTTaskDatabaseService cftTaskDatabaseService;

    @InjectMocks
    private TaskReconfigurationService taskReconfigurationService;


    @Test
    void should_mark_tasks_to_reconfigure_if_task_resource_is_not_already_marked() {

        OffsetDateTime todayTestDatetime = OffsetDateTime.now();
        List<TaskFilter> taskFilters = createTaskFilters();

        List<TaskResource> taskResources = taskResources(null);
        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(taskResources);
        when(cftTaskDatabaseService.findByIdAndObtainPessimisticWriteLock(anyString()))
            .thenReturn(Optional.of(taskResources.get(0)))
            .thenReturn(Optional.of(taskResources.get(1)));

        List<TaskResource> taskResourcesMarked = taskReconfigurationService.markTasksToReconfigure(taskFilters);

        taskResourcesMarked.stream().forEach(taskResource -> {
            assertNotNull(taskResource.getReconfigureRequestTime());
            assertTrue(taskResource.getReconfigureRequestTime().isAfter(todayTestDatetime));
        });

    }

    @Test
    void should_not_mark_tasks_to_reconfigure_if_task_resource_is_not_active() {
        List<TaskFilter> taskFilters = createTaskFilters();

        List<TaskResource> taskResources = cancelledTaskResources();
        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(taskResources);

        List<TaskResource> taskResourcesMarked = taskReconfigurationService.markTasksToReconfigure(taskFilters);

        taskResourcesMarked.stream().forEach(taskResource -> {
            assertNull(taskResource.getReconfigureRequestTime());
        });

    }

    @Test
    void should_not_mark_tasks_to_reconfigure_if_task_resource_is_already_marked_to_configure() {
        List<TaskFilter> taskFilters = createTaskFilters();

        when(cftTaskDatabaseService.getActiveTasksByCaseIdsAndReconfigureRequestTimeIsNull(
            anyList(), anyList())).thenReturn(List.of());

        List<TaskResource> taskResourcesMarked = taskReconfigurationService.markTasksToReconfigure(taskFilters);

        assertEquals(0, taskResourcesMarked.size());
    }

    private List<TaskFilter> createTaskFilters() {
        TaskFilter filter = new TaskFilter("case_id", List.of("1234", "4567"), TaskFilterOperator.IN);
        return List.of(filter);
    }

    private List<TaskResource> taskResources(OffsetDateTime reconfigureTime) {
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
            taskResource1.setReconfigureRequestTime(reconfigureTime);
            taskResource2.setReconfigureRequestTime(reconfigureTime);
        }
        return List.of(taskResource1, taskResource2);
    }

    private List<TaskResource> cancelledTaskResources() {
        TaskResource taskResource1 = new TaskResource(
            "5678",
            "someTaskName",
            "someTaskType",
            CFTTaskState.CANCELLED,
            "someCaseId"
        );
        TaskResource taskResource2 = new TaskResource(
            "6789",
            "someTaskName",
            "someTaskType",
            CFTTaskState.CANCELLED,
            "someCaseId"
        );
        return List.of(taskResource1, taskResource2);
    }
}
