package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class MIReplicaReportingMarkAndRefreshTest extends ReplicaBaseTest {

    /*@ParameterizedTest
    @CsvSource(value = {
        //"1, 1, 1",
        //"1, 2, 1",
        "2, 1, 1"
    })
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void should_test_refresh_report_tasks_for_positive_scenarios(Integer taskResourcesToCreate,
                                                                        Integer maxRowsToProcess,
                                                                        Integer expectedProcessed) {
        processReportingRecords(taskResourcesToCreate, maxRowsToProcess, expectedProcessed);

    }

    @ParameterizedTest
    @CsvSource(value = {
        //"0, 2, 0",
        //"1, 0, 1",
        "1, -2, 1"
    })
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void should_test_refresh_report_tasks_for_negative_scenarios(Integer taskResourcesToCreate,
                                                                        Integer maxRowsToProcess,
                                                                        Integer expectedProcessed) {
        processReportingRecords(taskResourcesToCreate, maxRowsToProcess, expectedProcessed);

    }*/

    private void processReportingRecords(Integer taskResourcesToCreate, Integer maxRowsToProcess,
                                         Integer expectedProcessed) {
        List<TaskResource> tasks = new ArrayList<>();
        IntStream.range(0, taskResourcesToCreate).forEach(x -> {
            TaskResource taskResource = createAndAssignTask();
            log.info(taskResource.toString());
            tasks.add(taskResource);
        });

        tasks.forEach(task -> await().ignoreException(AssertionFailedError.class)
            .pollInterval(3, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(task.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    ReportableTaskResource reportableTaskResource = reportableTaskList.get(0);
                    assertEquals(task.getTaskId(), reportableTaskResource.getTaskId());
                    assertEquals(task.getState().getValue(), reportableTaskResource.getState());

                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingServiceForTest.findByAssignmentsTaskId(task.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());

                    return true;
                }));

        List<String> taskIds = tasks.stream().map(TaskResource::getTaskId).toList();

        List<Timestamp> taskRefreshTimestamps = callGetReportRefreshRequestTimes(taskIds);
        taskRefreshTimestamps.forEach(Assertions::assertNull);

        callMarkReportTasksForRefresh(null, null, null,
                                      null, null, OffsetDateTime.now());

        taskRefreshTimestamps = callGetReportRefreshRequestTimes(taskIds);
        long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(taskResourcesToCreate,
                                (int) count, String.format("Should mark all %s tasks:", taskResourcesToCreate));

        callRefreshReportTasks(maxRowsToProcess);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(5, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    List<Timestamp> taskRefreshTimestampList = callGetReportRefreshRequestTimes(taskIds);
                    long countNotRefreshed = taskRefreshTimestampList.stream().map(Objects::nonNull).count();
                    Assertions.assertEquals(expectedProcessed, taskResourcesToCreate - (int) countNotRefreshed,
                                            String.format("Should refresh %s tasks:", expectedProcessed));
                    return true;
                });

        AtomicInteger reportableTasksRefreshedCount = new AtomicInteger();
        AtomicInteger taskAssignmentsRefreshedCount = new AtomicInteger();

        taskIds.forEach(x -> {
            List<ReportableTaskResource> reportableTaskList
                = miReportingServiceForTest.findByReportingTaskId(x);

            assertFalse(reportableTaskList.isEmpty());
            assertEquals(1, reportableTaskList.size());
            assertEquals(x, reportableTaskList.get(0).getTaskId());
            if (reportableTaskList.get(0).getReportRefreshTime() != null) {
                reportableTasksRefreshedCount.getAndIncrement();
            }

            List<TaskAssignmentsResource> taskAssignmentsList
                = miReportingServiceForTest.findByAssignmentsTaskId(x);

            assertFalse(taskAssignmentsList.isEmpty());
            assertEquals(1, taskAssignmentsList.size());
            assertEquals(x, taskAssignmentsList.get(0).getTaskId());
            if (taskAssignmentsList.get(0).getReportRefreshTime() != null) {
                taskAssignmentsRefreshedCount.getAndIncrement();
            }
        });

        assertEquals(expectedProcessed, reportableTasksRefreshedCount.get());
        assertEquals(expectedProcessed, taskAssignmentsRefreshedCount.get());
    }

    private TaskResource createAndAssignTask() {

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someNewCaseId",
            "someJurisdiction",
            "someLocation",
            "someRoleCategory",
            "someTaskName");

        taskResource.setDueDateTime(OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
        taskResource.setCreated(OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"));
        taskResource.setAssignee("someAssignee");
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setState(ASSIGNED);
        return taskResourceRepository.save(taskResource);
    }
}
