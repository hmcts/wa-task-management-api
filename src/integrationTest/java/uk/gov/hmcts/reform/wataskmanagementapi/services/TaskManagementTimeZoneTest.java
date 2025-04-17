package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class TaskManagementTimeZoneTest extends ReplicaBaseTest {

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "BST"})
    void when_timezone_changes_all_timestamp_attributes_should_behave_consistently(String timeZone) {

        TaskResource taskResource;
        log.info("TimeZone ({})", timeZone);

        if ("UTC".equals(timeZone)) {
            taskResource = createAndSaveTask(OffsetDateTime.of(2024, 10, 27, 02,
                                                               00, 00, 0, ZoneOffset.UTC));
        } else {
            taskResource = createAndSaveTask(OffsetDateTime.of(2024, 03, 31,
                                                               01, 00, 00, 0,
                                                               ZoneOffset.of("+01:00")));
        }

        AtomicReference<TaskHistoryResource> taskHistoryResource = new AtomicReference<>();

        AtomicReference<ReportableTaskResource> reportableTaskResource = new AtomicReference<>();

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(taskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());

                    taskHistoryResource.set(taskHistoryResourceList.get(0));

                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());

                    reportableTaskResource.set(reportableTaskList.get(0));

                    return true;
                });

        assertTrue(taskResource.getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getUpdated()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getCreated().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getCreated()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getPriorityDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getAssignmentExpiry()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getNextHearingDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getReconfigureRequestTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getReconfigureRequestTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getLastReconfigurationTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getLastReconfigurationTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(taskHistoryResource.get().getDueDateTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getUpdated().truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getCreated().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getCreated().truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getPriorityDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getAssignmentExpiry()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getNextHearingDate()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getReconfigureRequestTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getReconfigureRequestTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getLastReconfigurationTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getLastReconfigurationTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));
        assertTrue(taskResource.getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                       .isEqual(reportableTaskResource.get().getDueDateTime()
                                    .truncatedTo(ChronoUnit.SECONDS)));



    }

    private TaskResource createAndSaveTask(OffsetDateTime now) {

        log.info("Current time: {}", now);

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
                now
        );
        taskResource.setCreated(now);
        taskResource.setPriorityDate(now);
        taskResource.setLastUpdatedTimestamp(now);
        taskResource.setLastUpdatedAction("Configure");
        taskResource.setLastUpdatedUser("System");
        taskResource.setDueDateTime(now.plusDays(7));
        taskResource.setAssignmentExpiry(now.plusDays(7));
        taskResource.setNextHearingDate(now.plusDays(30));
        taskResource.setReconfigureRequestTime(now.minusDays(1));
        taskResource.setLastReconfigurationTime(now.minusDays(2));
        return taskResourceRepository.save(taskResource);
    }
}
