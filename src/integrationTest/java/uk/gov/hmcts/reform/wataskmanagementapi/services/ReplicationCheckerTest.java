package uk.gov.hmcts.reform.wataskmanagementapi.services;

import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ReplicationChecker;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReplicationCheckerTest extends ReplicaBaseTest {

    @Autowired
    ReplicationChecker replicationChecker;

    @Test
    void should_save_task_and_get_task_from_replica_tables() {
        TaskResource taskResource = createAndSaveTask();

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(taskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals(1, taskHistoryResourceList.size());
                    assertEquals(taskResource.getTaskId(), taskHistoryResourceList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskHistoryResourceList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), taskHistoryResourceList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), taskHistoryResourceList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp()
                        .isEqual(taskHistoryResourceList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), taskHistoryResourceList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), taskHistoryResourceList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), taskHistoryResourceList.get(0).getUpdateAction());

                    return true;
                });

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.PERFORM_REPLICATION_CHECK).build(),
            List.of()
        );

        Map<String, Object> resourceMap = replicationChecker.performOperation(
            request
        ).getResponseMap();

        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());

    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-15T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction("Configure");
        return taskResourceRepository.save(taskResource);
    }
}
