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
import java.util.List;
import java.util.TimeZone;
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
public class TaskManagementTimeZoneTest extends ReplicaBaseTest {

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "GMT+01:00"})
    void when_timezone_changes_all_timestamp_attributes_should_behave_consistently(String timeZone) {
        //TimeZone.setDefault(TimeZone.getTimeZone(timeZone));

        log.info("TimeZone ({}): {}", timeZone, OffsetDateTime.now());
        TaskResource taskResource = createAndSaveTask();

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(20, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(taskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertTrue(taskResource.getLastUpdatedTimestamp()
                                   .isEqual(taskHistoryResourceList.get(0).getUpdated()));
                    assertTrue(taskResource.getCreated().isEqual(taskHistoryResourceList.get(0).getCreated()));
                    assertTrue(taskResource.getPriorityDate()
                                   .isEqual(taskHistoryResourceList.get(0).getPriorityDate()));
                    assertTrue(taskResource.getAssignmentExpiry()
                                   .isEqual(taskHistoryResourceList.get(0).getAssignmentExpiry()));
                    assertTrue(taskResource.getNextHearingDate()
                                   .isEqual(taskHistoryResourceList.get(0).getNextHearingDate()));
                    assertTrue(taskResource.getReconfigureRequestTime()
                                   .isEqual(taskHistoryResourceList.get(0).getReconfigureRequestTime()));
                    assertTrue(taskResource.getLastReconfigurationTime()
                                   .isEqual(taskHistoryResourceList.get(0).getLastReconfigurationTime()));
                    assertTrue(taskResource.getDueDateTime().isEqual(taskHistoryResourceList.get(0).getDueDateTime()));

                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertTrue(taskResource.getLastUpdatedTimestamp().isEqual(reportableTaskList.get(0).getUpdated()));
                    assertTrue(taskResource.getCreated().isEqual(reportableTaskList.get(0).getCreated()));
                    assertTrue(taskResource.getPriorityDate().isEqual(reportableTaskList.get(0).getPriorityDate()));
                    assertTrue(taskResource.getAssignmentExpiry()
                                   .isEqual(reportableTaskList.get(0).getAssignmentExpiry()));
                    assertTrue(taskResource.getNextHearingDate()
                                   .isEqual(reportableTaskList.get(0).getNextHearingDate()));
                    assertTrue(taskResource.getReconfigureRequestTime()
                                   .isEqual(reportableTaskList.get(0).getReconfigureRequestTime()));
                    assertTrue(taskResource.getLastReconfigurationTime()
                                   .isEqual(reportableTaskList.get(0).getLastReconfigurationTime()));
                    assertTrue(taskResource.getDueDateTime().isEqual(reportableTaskList.get(0).getDueDateTime()));

                    return true;
                });

    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.now()
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.now());
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
        taskResource.setLastUpdatedUser("System");
        taskResource.setDueDateTime(OffsetDateTime.now().plusDays(7));
        taskResource.setAssignmentExpiry(OffsetDateTime.now().plusDays(7));
        taskResource.setNextHearingDate(OffsetDateTime.now().plusDays(30));
        taskResource.setReconfigureRequestTime(OffsetDateTime.now().minusDays(1));
        taskResource.setLastReconfigurationTime(OffsetDateTime.now().minusDays(2));
        return taskResourceRepository.save(taskResource);
    }
}
