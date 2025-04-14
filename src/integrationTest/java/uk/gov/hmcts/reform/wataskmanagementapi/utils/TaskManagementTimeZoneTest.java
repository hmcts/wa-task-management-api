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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

                    assertThat(taskResource.getLastUpdatedTimestamp())
                        .isCloseTo(taskHistoryResourceList.get(0).getUpdated(), within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getCreated())
                        .isCloseTo(taskHistoryResourceList.get(0).getCreated(), within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getPriorityDate())
                        .isCloseTo(taskHistoryResourceList.get(0).getPriorityDate(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getAssignmentExpiry())
                        .isCloseTo(taskHistoryResourceList.get(0).getAssignmentExpiry(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getNextHearingDate())
                        .isCloseTo(taskHistoryResourceList.get(0).getNextHearingDate(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getReconfigureRequestTime())
                        .isCloseTo(taskHistoryResourceList.get(0).getReconfigureRequestTime(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getLastReconfigurationTime())
                        .isCloseTo(taskHistoryResourceList.get(0).getLastReconfigurationTime(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getDueDateTime())
                        .isCloseTo(taskHistoryResourceList.get(0).getDueDateTime(),
                                   within(100, ChronoUnit.MILLIS));

                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertThat(taskResource.getLastUpdatedTimestamp())
                        .isCloseTo(reportableTaskList.get(0).getUpdated(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getCreated())
                        .isCloseTo(reportableTaskList.get(0).getCreated(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getPriorityDate())
                        .isCloseTo(reportableTaskList.get(0).getPriorityDate(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getAssignmentExpiry())
                        .isCloseTo(reportableTaskList.get(0).getAssignmentExpiry(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getNextHearingDate())
                        .isCloseTo(reportableTaskList.get(0).getNextHearingDate(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getReconfigureRequestTime())
                        .isCloseTo(reportableTaskList.get(0).getReconfigureRequestTime(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getLastReconfigurationTime())
                        .isCloseTo(reportableTaskList.get(0).getLastReconfigurationTime(),
                                   within(100, ChronoUnit.MILLIS));
                    assertThat(taskResource.getDueDateTime())
                        .isCloseTo(reportableTaskList.get(0).getDueDateTime(), within(100, ChronoUnit.MILLIS));

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
