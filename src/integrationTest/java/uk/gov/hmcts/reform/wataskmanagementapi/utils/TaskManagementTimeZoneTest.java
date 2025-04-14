package uk.gov.hmcts.reform.wataskmanagementapi.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.services.ReplicaBaseTest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

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
            taskResource = createAndSaveTask(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            taskResource = createAndSaveTask(OffsetDateTime.now(ZoneOffset.of("+01:00")));
        }


        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(taskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());

                    assertTrue(taskResource.getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getUpdated()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getCreated().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getCreated()
                                               .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getPriorityDate()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getAssignmentExpiry()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getNextHearingDate()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getReconfigureRequestTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getReconfigureRequestTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getLastReconfigurationTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getLastReconfigurationTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(taskHistoryResourceList.get(0).getDueDateTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));

                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertTrue(taskResource.getLastUpdatedTimestamp().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getUpdated().truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getCreated().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getCreated().truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getPriorityDate().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getPriorityDate()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getAssignmentExpiry().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getAssignmentExpiry()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getNextHearingDate().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getNextHearingDate()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getReconfigureRequestTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getReconfigureRequestTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getLastReconfigurationTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getLastReconfigurationTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));
                    assertTrue(taskResource.getDueDateTime().truncatedTo(ChronoUnit.SECONDS)
                                   .isEqual(reportableTaskList.get(0).getDueDateTime()
                                                .truncatedTo(ChronoUnit.SECONDS)));

                    return true;
                });

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
