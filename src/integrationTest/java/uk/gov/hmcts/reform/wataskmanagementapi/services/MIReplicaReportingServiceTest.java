package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.db.MIReplicaDBDao;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.NoteResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

/**
 * We test logical replication in here.
 */
@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
class MIReplicaReportingServiceTest extends ReplicaBaseTest {


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
    }

    @Test
    void should_save_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveTask();

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp().isEqual(reportableTaskList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ADD, false",
        "UPDATE, false",
        "ADD, true",
        "UPDATE, true"
    })
    void should_save_task_and_get_task_from_replica_tables_with_new_columns(String operation, boolean required) {
        TaskResource taskResource;
        if ("UPDATE".equals(operation)) {
            taskResource = buildTaskResource(3,5);
            taskResource = taskResourceRepository.save(taskResource);
            checkHistory(taskResource.getTaskId(), 1);
            log.info("Operation UPDATE and Check History Done with 1 record");
            taskResource.setState(CFTTaskState.COMPLETED);
            taskResource.setLastUpdatedAction("AutoAssign");
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        } else {
            taskResource = buildTaskResource(3,5);
        }

        addMissingParameters(taskResource, required);

        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        log.info("Operation {} and saved taskResource", operation);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(savedTaskResource.getTaskId());

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals("UPDATE".equals(operation) ? 2 : 1, taskHistoryResourceList.size());

                    TaskHistoryResource taskHistoryResource = "UPDATE".equals(operation)
                        ? taskHistoryResourceList.get(1) : taskHistoryResourceList.get(0);
                    assertEquals(savedTaskResource.getTaskId(), taskHistoryResource.getTaskId());
                    assertEquals(savedTaskResource.getDescription(), taskHistoryResource.getDescription(),
                                 "Discription should match");
                    log.info("Discription should match : {}, {}",
                             savedTaskResource.getDescription(), taskHistoryResource.getDescription());
                    assertEquals(savedTaskResource.getRegionName(), taskHistoryResource.getRegionName(),
                                 "RegionName should match");
                    log.info("RegionName should match : {}, {} ",
                             savedTaskResource.getRegionName(), taskHistoryResource.getRegionName());
                    assertEquals(savedTaskResource.getLocationName(), taskHistoryResource.getLocationName(),
                                 "LocationName should match");
                    log.info("LocationName should match : {}, {} ",
                             savedTaskResource.getLocationName(), taskHistoryResource.getLocationName());
                    assertEquals(savedTaskResource.getNotes(), taskHistoryResource.getNotes(),
                                 "Notes should match");
                    log.info("Notes should match : {}, {} ",
                             savedTaskResource.getNotes(), taskHistoryResource.getNotes());
                    assertEquals(savedTaskResource.getAdditionalProperties(),
                                   taskHistoryResource.getAdditionalProperties(),
                                 "Additional Properties should match");
                    log.info("Additional Properties should match : {}, {} ",
                             savedTaskResource.getAdditionalProperties(),
                             taskHistoryResource.getAdditionalProperties());
                    assertEquals(savedTaskResource.getReconfigureRequestTime(),
                                   taskHistoryResource.getReconfigureRequestTime(),
                                   "Reconfiguration Request Time should match");
                    log.info("Reconfiguration Request Time should match : {}, {} ",
                             savedTaskResource.getReconfigureRequestTime(),
                             taskHistoryResource.getReconfigureRequestTime());
                    assertEquals(savedTaskResource.getLastReconfigurationTime(),
                                   taskHistoryResource.getLastReconfigurationTime(),
                                 "Last Reconfiguration Time should match");
                    log.info("Last Reconfiguration Time should match : {}, {} ",
                             savedTaskResource.getLastReconfigurationTime(),
                             taskHistoryResource.getLastReconfigurationTime());
                    assertEquals(savedTaskResource.getNextHearingId(), taskHistoryResource.getNextHearingId(),
                                 "NextHearingID should match");
                    assertEquals(savedTaskResource.getNextHearingDate(),
                                   taskHistoryResource.getNextHearingDate(),
                                 "Next Hearing Data should Match");
                    assertEquals(savedTaskResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                                   taskHistoryResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                                 "Get Priority Date should match");

                    return true;
                });

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(savedTaskResource.getTaskId());
                    log.info("Operation {} and reportableTaskList size {}", operation,
                             reportableTaskList.size());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());

                    log.info("Operation {} and reportableTask {}", operation,
                             reportableTaskList.get(0).toString());

                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(savedTaskResource.getDescription(), reportableTaskList.get(0).getDescription());
                    assertEquals(savedTaskResource.getRegionName(), reportableTaskList.get(0).getRegionName());
                    assertEquals(savedTaskResource.getLocationName(), reportableTaskList.get(0).getLocationName());
                    assertEquals(savedTaskResource.getNotes(), reportableTaskList.get(0).getNotes());
                    assertEquals(savedTaskResource.getAdditionalProperties(),
                                 reportableTaskList.get(0).getAdditionalProperties());
                    assertEquals(savedTaskResource.getReconfigureRequestTime(),
                                   reportableTaskList.get(0).getReconfigureRequestTime());
                    assertEquals(savedTaskResource.getLastReconfigurationTime(),
                                   reportableTaskList.get(0).getLastReconfigurationTime());
                    assertEquals(savedTaskResource.getNextHearingId(), reportableTaskList.get(0).getNextHearingId());
                    assertEquals(savedTaskResource.getNextHearingDate(),
                                   reportableTaskList.get(0).getNextHearingDate());
                    assertEquals(savedTaskResource.getPriorityDate().atZoneSameInstant(ZoneId.of("Z")),
                               reportableTaskList.get(0).getPriorityDate().atZoneSameInstant(ZoneId.of("Z")));

                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "LEGAL_OPERATIONS,Legal Operations,PRIVATELAW,Private Law,PRLAPPS,Private Law,ASSIGNED,AutoAssign,Assigned",
        "CTSC,CTSC,CIVIL,Civil,CIVIL,Civil,UNASSIGNED,Configure,Unassigned",
        "JUDICIAL,Judicial,IA,Immigration and Asylum,Asylum,Asylum,ASSIGNED,AutoAssign,Assigned",
        "ADMIN,Admin,PUBLICLAW,Public Law,PUBLICLAW,Public Law,UNASSIGNED,Configure,Unassigned",
        ",,WA,WA,WaCaseType,WaCaseType,ASSIGNED,AutoAssign,Assigned",
        "TEST,TEST,TEST,TEST,TEST,TEST,UNASSIGNED,Configure,Unassigned"
    })
    void should_save_task_and_get_transformed_labels_from_reportable_task(
        String taskRoleCategory, String reportableTaskRoleCategoryLabel,
        String taskJurisdiction, String reportableTaskJurisdictionLabel,
        String taskCaseTypeId, String reportableTaskCaseTypeLabel,
        String taskState, String lastUpdatedAction, String reportableTaskStateLabel) {
        TaskResource taskResource = buildTaskResource(4,10);
        taskResource.setRoleCategory(taskRoleCategory);
        taskResource.setJurisdiction(taskJurisdiction);
        taskResource.setCaseTypeId(taskCaseTypeId);
        taskResource.setState(CFTTaskState.valueOf(taskState));
        taskResource.setLastUpdatedAction(lastUpdatedAction);
        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        checkHistory(savedTaskResource.getTaskId(), 1);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    log.info("Found reportable task:{}",reportableTaskList.get(0));
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(reportableTaskRoleCategoryLabel, reportableTaskList.get(0).getRoleCategoryLabel());
                    assertEquals(reportableTaskJurisdictionLabel, reportableTaskList.get(0).getJurisdictionLabel());
                    assertEquals(reportableTaskCaseTypeLabel, reportableTaskList.get(0).getCaseTypeLabel());
                    assertEquals(reportableTaskStateLabel, reportableTaskList.get(0).getStateLabel());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "ASSIGNED,AutoAssign,Assigned",
        "UNASSIGNED,Configure,Unassigned",
        "COMPLETED,AutoAssign,Completed",
        "CANCELLED,Configure,Cancelled",
        "TERMINATED,AutoAssign,Terminated",
        "PENDING_RECONFIGURATION,Configure,Pending Reconfiguration",
        "PENDING_AUTO_ASSIGN,Configure,PENDING_AUTO_ASSIGN"
    })
    void should_save_task_and_get_transformed_state_label_from_reportable_task(
        String taskState, String lastUpdatedAction, String reportableTaskStateLabel) {
        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        checkHistory(taskId, 1);

        taskResource.setState(CFTTaskState.valueOf(taskState));
        taskResource.setLastUpdatedAction(lastUpdatedAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        TaskResource savedTaskResource = taskResourceRepository.save(taskResource);
        checkHistory(taskId, 2);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(savedTaskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(savedTaskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(reportableTaskStateLabel, reportableTaskList.get(0).getStateLabel());
                    return true;
                });
    }

    @Test
    void should_save_auto_assigned_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveAndAssignTask();
        checkHistory(taskResource.getTaskId(), 1);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertTrue(taskResource.getLastUpdatedTimestamp().isEqual(reportableTaskList.get(0).getUpdated()));
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());
                    assertEquals(0, reportableTaskList.get(0).getWaitTimeDays());
                    assertEquals("00:00:00", reportableTaskList.get(0).getWaitTime());
                    assertTrue(reportableTaskList.get(0).getFirstAssignedDateTime()
                        .isEqual(reportableTaskList.get(0).getUpdated()));
                    assertNotNull(reportableTaskList.get(0).getFirstAssignedDate());
                    assertEquals(0, reportableTaskList.get(0).getNumberOfReassignments());

                    return true;
                });
    }

    @Test
    void should_save_AutoAssign_task_and_get_task_from_reportable_task() {
        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        checkHistory(taskId, 1);

        taskResource.setState(ASSIGNED);
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-07T20:15:50.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskId, 2);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(2, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskResource.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskResource.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), reportableTaskList.get(0).getTaskName());
                    assertEquals(taskResource.getTitle(), reportableTaskList.get(0).getTaskTitle());
                    assertEquals(taskResource.getAssignee(), reportableTaskList.get(0).getAssignee());
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    assertEquals(2, reportableTaskList.get(0).getWaitTimeDays());
                    assertEquals("2 days 00:00:05", reportableTaskList.get(0).getWaitTime());

                    return true;
                });

        taskResource.setLastUpdatedAction("Complete");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-06-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    assertEquals("No", reportableTaskList.get(0).getIsWithinSla());
                    assertEquals(31, reportableTaskList.get(0).getDueDateToCompletedDiffDays());
                    assertEquals(35, reportableTaskList.get(0).getProcessingTimeDays());
                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_task_assignments() {
        TaskResource taskResource = createAndAssignTask();
        checkHistory(taskResource.getTaskId(), 1);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingServiceForTest.findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "COMPLETED,Complete,COMPLETED, COMPLETED",
        "ASSIGNED,AutoUnassignAssign,REASSIGNED, null",
        "ASSIGNED,UnassignAssign,REASSIGNED, null",
        "ASSIGNED,UnassignClaim,REASSIGNED, null",
        "UNASSIGNED,UnclaimAssign,REASSIGNED, null",
        "UNASSIGNED,Unassign,UNASSIGNED, null",
        "UNASSIGNED,AutoUnassign,UNASSIGNED, null",
        "UNASSIGNED,Unclaim,UNCLAIMED, null",
        "ASSIGNED,AutoCancel,CANCELLED, AUTO_CANCELLED",
        "ASSIGNED,Cancel,CANCELLED, USER_CANCELLED",
    })
    void should_save_task_and_check_task_assignments(String newState, String lastAction, String endReason,
                                                     String finalState) {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = createAndSaveThisTask(taskId, "someTaskName",
            UNASSIGNED, "Configure");

        taskResource.setAssignee("someAssignee");
        taskResource.setLastUpdatedAction("AutoAssign");
        taskResource.setState(ASSIGNED);
        taskResourceRepository.save(taskResource);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingServiceForTest.findByAssignmentsTaskId(taskId);

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(1, taskAssignmentsList.size());
                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals(taskResource.getAssignee(), taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertNull(taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });

        if (lastAction.matches("Unassign|Unclaim|AutoUnassign")) {
            taskResource.setAssignee(null);
        }
        if (lastAction.matches("AutoUnassignAssign|UnassignAssign|UnassignClaim|UnclaimAssign|Assign")) {
            taskResource.setAssignee("newAssignee");
        }
        //"2023-03-29T20:15:45.345875+01:00"
        if (lastAction.equals("Complete")) {
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:55.345875+01:00"));
        } else {
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00"));
        }
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setState(CFTTaskState.valueOf(newState));
        taskResourceRepository.save(taskResource);

        checkHistory(taskId, 3);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingServiceForTest.findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    if (lastAction.matches("AutoUnassignAssign|UnassignAssign|UnassignClaim|UnclaimAssign|Assign")) {
                        assertEquals(2, taskAssignmentsList.size());
                    } else {
                        assertEquals(1, taskAssignmentsList.size());
                    }

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertNotNull(taskAssignmentsList.get(0).getAssignmentEnd());
                    assertEquals(endReason, taskAssignmentsList.get(0).getAssignmentEndReason());

                    return true;
                });

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    if (lastAction.matches("Complete|AutoCancel|Cancel")) {
                        assertEquals(finalState, reportableTaskList.get(0).getFinalStateLabel());
                        if (lastAction.equals("Complete")) {

                            assertEquals("No", reportableTaskList.get(0).getIsWithinSla());
                            assertEquals(2, reportableTaskList.get(0).getDueDateToCompletedDiffDays());
                            assertEquals(15, reportableTaskList.get(0).getProcessingTimeDays());
                        }
                    } else {
                        assertNull(reportableTaskList.get(0).getFinalStateLabel());
                        assertNull(reportableTaskList.get(0).getIsWithinSla());
                    }
                    assertNotNull(reportableTaskList.get(0).getLastUpdatedDate());

                    List<TaskHistoryResource> taskHistoryList
                        = miReportingServiceForTest.findByTaskId(taskId);
                    assertEquals(3, taskHistoryList.size());
                    return true;
                });

        if (lastAction.equals("Complete")) {
            await().ignoreException(AssertionFailedError.class)
                .pollInterval(1, SECONDS)
                .atMost(10, SECONDS)
                .until(
                    () -> {
                        List<ReportableTaskResource> reportableTaskList
                            = miReportingServiceForTest.findByReportingTaskId(taskId);

                        assertEquals("9 days 00:00:10", reportableTaskList.get(0).getHandlingTime());
                        assertTrue(reportableTaskList.get(0).getProcessingTime().startsWith("15 days"));
                        assertEquals("-2 days -00:00:10", reportableTaskList.get(0).getDueDateToCompletedDiffTime());
                        return true;
                    });
        }
    }

    @Test
    void should_save_task_and_record_multiple_task_assignments() {
        TaskResource taskResource = createAndAssignTask();
        checkHistory(taskResource.getTaskId(), 1);
        taskResource.setLastUpdatedAction("Unclaim");
        taskResource.setState(CFTTaskState.UNASSIGNED);
        taskResource.setAssignee(null);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 2);

        taskResource.setLastUpdatedAction("Assign");
        taskResource.setState(CFTTaskState.ASSIGNED);
        taskResource.setAssignee("NewAssignee");
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-12T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 3);

        taskResource.setLastUpdatedAction("Complete");
        taskResource.setState(CFTTaskState.COMPLETED);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-04-17T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
        checkHistory(taskResource.getTaskId(), 4);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingServiceForTest.findByAssignmentsTaskId(taskResource.getTaskId());

                    assertFalse(taskAssignmentsList.isEmpty());
                    assertEquals(2, taskAssignmentsList.size());

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(0).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(0).getTaskName());
                    assertEquals("someAssignee", taskAssignmentsList.get(0).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(0).getService());
                    assertTrue(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(0).getAssignmentStart()));
                    assertTrue(OffsetDateTime.parse("2023-04-07T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(0).getAssignmentEnd()));
                    assertEquals("UNCLAIMED", taskAssignmentsList.get(0).getAssignmentEndReason());

                    assertEquals(taskResource.getTaskId(), taskAssignmentsList.get(1).getTaskId());
                    assertEquals(taskResource.getTaskName(), taskAssignmentsList.get(1).getTaskName());
                    assertEquals("NewAssignee", taskAssignmentsList.get(1).getAssignee());
                    assertEquals(taskResource.getJurisdiction(), taskAssignmentsList.get(1).getService());
                    assertTrue(OffsetDateTime.parse("2023-04-12T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(1).getAssignmentStart()));
                    assertTrue(OffsetDateTime.parse("2023-04-17T20:15:45.345875+01:00").isEqual(
                        taskAssignmentsList.get(1).getAssignmentEnd()));
                    assertEquals("COMPLETED", taskAssignmentsList.get(1).getAssignmentEndReason());

                    return true;
                });
    }

    @Test
    void given_zero_publications_should_return_false() {
        TaskResourceRepository mocktaskResourceRepository = mock(TaskResourceRepository.class);
        when(mocktaskResourceRepository.countPublications()).thenReturn(0);
        subscriptionCreatorForTest = new SubscriptionCreator(TEST_REPLICA_DB_USER, TEST_REPLICA_DB_PASS,
            TEST_PRIMARY_DB_USER, TEST_PRIMARY_DB_PASS, TEST_PUBLICATION_URL, ENVIRONMENT);
        MIReportingService service = new MIReportingService(null, mocktaskResourceRepository,
            reportableTaskRepository,
            taskAssignmentsRepository,
            subscriptionCreatorForTest);

        assertFalse(service.isPublicationPresent());
    }

    @Test
    void given_unknown_task_id_what_happens() {
        List<TaskHistoryResource> taskHistoryResourceList
            = miReportingServiceForTest.findByTaskId("1111111");
        assertTrue(taskHistoryResourceList.isEmpty());
    }


    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign",
        "ASSIGNED,Configure",
        "UNASSIGNED,Claim",
        "UNASSIGNED,AutoAssign"
    })
    void should_report_incomplete_task_history(String initialState, String lastAction) {
        String taskId = UUID.randomUUID().toString();
        createAndSaveThisTask(taskId, "someTaskName",
            CFTTaskState.valueOf(initialState), lastAction);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskId);

                    if ((initialState.equals("UNASSIGNED") && lastAction.equals("Configure"))
                        || (initialState.equals("ASSIGNED") && lastAction.equals("AutoAssign"))) {
                        assertFalse(reportableTaskList.isEmpty());
                        assertFalse(containerReplica.getLogs().contains(taskId + " : Task with an incomplete history"));
                    } else {
                        assertTrue(reportableTaskList.isEmpty());
                        assertTrue(containerReplica.getLogs().contains(taskId + " : Task with an incomplete history"));
                    }
                    return true;
                });

    }

    private TaskResource buildTaskResource(int daysUntilDue, int daysUntilPriority) {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.now().plusDays(daysUntilDue).withHour(10).withMinute(0).withSecond(0).withNano(0)
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(
            OffsetDateTime.now().plusDays(daysUntilPriority).withHour(10).withMinute(0).withSecond(0).withNano(0));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
        return taskResource;
    }

    private void addMissingParameters(TaskResource taskResource, boolean required) {
        taskResource.setDescription(required
            ? "[Decide an application](/case/WA/WaCaseType/${[CASE_REFERENCE]}/trigger/decideAnApplication)" : null);
        List<NoteResource> notesList = new ArrayList<>();
        final NoteResource noteResource = new NoteResource(
            "someCode",
            "noteTypeVal",
            "userVal",
            "someContent"
        );
        notesList.add(noteResource);
        taskResource.setNotes(required ? notesList : null);
        taskResource.setRegion(required ? "Wales" : null);
        taskResource.setLocationName(required ? "Cardiff" : null);
        taskResource.setAdditionalProperties(required ? Map.of(
            "key1", "value1",
            "key2", "value2",
            "key3", "value3",
            "key4", "value4"
        ) : null);
        taskResource.setReconfigureRequestTime(required
            ? OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0) : null);
        taskResource.setLastReconfigurationTime(required
            ? OffsetDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0) : null);
        taskResource.setNextHearingId(required ? "W-CA-1234" : null);
        taskResource.setNextHearingDate(required
            ? OffsetDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0) :  null);
    }

    private TaskResource createAndSaveTask(String caseId,
                                           String taskId,
                                           String taskState,
                                           OffsetDateTime created,
                                           String caseTypeId,
                                           String jurisdiction) {

        TaskResource taskResource = new TaskResource(
            taskId,
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            caseId,
            OffsetDateTime.now().plusDays(4).withHour(10).withMinute(0).withSecond(0).withNano(0)
        );
        taskResource.setCreated(created);
        taskResource.setPriorityDate(OffsetDateTime.now().plusDays(3).withHour(10).withMinute(0).withSecond(0)
                                         .withNano(0));
        taskResource.setJurisdiction(jurisdiction);
        taskResource.setCaseTypeId(caseTypeId);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
        taskResource = taskResourceRepository.save(taskResource);

        if (!"UNASSIGNED".equals(taskState)) {
            taskResource.setState(CFTTaskState.valueOf(taskState));
            taskResource.setLastUpdatedAction("AutoAssign");
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
            addMissingParameters(taskResource, true);
            if ("TERMINATED".equals(taskState)) {
                taskResource.setTerminationReason("completed");
            }

            taskResource = taskResourceRepository.save(taskResource);
        }

        return taskResource;
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

    private TaskResource createAndSaveAndAssignTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            ASSIGNED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-15T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction("AutoAssign");
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveThisTask(String taskId, String taskName,
                                               CFTTaskState taskState, String lastAction) {
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            "someTaskType",
            taskState,
            "987654",
            OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
        taskResource.setJurisdiction("someJurisdiction");
        taskResource.setLocation("someLocation");
        taskResource.setRoleCategory("someRoleCategory");
        return taskResourceRepository.save(taskResource);
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

    private void createAndSaveTaskWithLastReconfigurationTime(String taskId, String taskName,
                                                              CFTTaskState taskState, String lastAction) {
        TaskResource taskResource = new TaskResource(
            taskId,
            taskName,
            "someTaskType",
            taskState,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastReconfigurationTime(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResourceRepository.save(taskResource);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign",
        "ASSIGNED,Configure"
    })
    void should_insert_first_task_and_update_reconfiguration_task(String state, String lastAction) {

        TaskResource taskResource = createAndSaveTask();
        String taskId = taskResource.getTaskId();
        createAndSaveTaskWithLastReconfigurationTime(taskId,
            "SecondTask", CFTTaskState.valueOf(state), lastAction);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    assertEquals("SecondTask", reportableTaskList.get(0).getTaskName());

                    List<TaskHistoryResource> taskHistoryList
                        = miReportingServiceForTest.findByTaskId(taskId);
                    assertEquals(2, taskHistoryList.size());
                    return true;
                });
    }

    @Test
    public void should_test_mark_functionality_with_refresh() {
        TaskResource t1 = createAndSaveTask("1000000001", UUID.randomUUID().toString(), "UNASSIGNED",
            OffsetDateTime.now().minusDays(20), "wa-ct", "wa-jd");
        TaskResource t2 = createAndSaveTask("1000000001", UUID.randomUUID().toString(), "UNASSIGNED",
                                             OffsetDateTime.now().minusDays(1), "wa-ct", "wa-jd");
        List<TaskResource> tasks = Arrays.asList(t1, t2);
        tasks.forEach(task -> await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(task.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(task.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(task.getState().getValue(), reportableTaskList.get(0).getState());

                    return true;
                }));

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                           containerReplica.getUsername(),
                                                           containerReplica.getPassword());

        // Mark filters to select both the tasks
        miReplicaDBDao.callMarkReportTasksForRefresh(null, null, null,
                                      null, null, OffsetDateTime.now());

        List<Timestamp>  taskRefreshTimestamps =
            miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(Arrays.asList(t1.getTaskId(), t2.getTaskId()));
        Long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(2, count);
        // verify marked times of both the tasks are different
        Assertions.assertEquals(taskRefreshTimestamps.get(0), taskRefreshTimestamps.get(1));

        Timestamp taskRequestRefreshTime = taskRefreshTimestamps.get(0);

        tasks.forEach(x -> {
            List<ReportableTaskResource> reportableTaskList
                = miReportingServiceForTest.findByReportingTaskId(x.getTaskId());

            assertFalse(reportableTaskList.isEmpty());
            assertEquals(1, reportableTaskList.size());
            assertEquals(x.getTaskId(), reportableTaskList.get(0).getTaskId());
            if (reportableTaskList.get(0).getReportRefreshTime() != null) {
                Timestamp reportRefreshTime = Timestamp.valueOf(reportableTaskList.get(0).getReportRefreshTime().toLocalDateTime());
                assertTrue(reportRefreshTime.after(taskRequestRefreshTime));
            }

            List<TaskAssignmentsResource> taskAssignmentsList
                = miReportingServiceForTest.findByAssignmentsTaskId(x.getTaskId());

            assertFalse(taskAssignmentsList.isEmpty());
            assertEquals(1, taskAssignmentsList.size());
            assertEquals(x.getTaskId(), taskAssignmentsList.get(0).getTaskId());
            if (taskAssignmentsList.get(0).getReportRefreshTime() != null) {
                Timestamp reportRefreshTime = Timestamp.valueOf(taskAssignmentsList.get(0).getReportRefreshTime().toLocalDateTime());
                assertTrue(reportRefreshTime.after(taskRequestRefreshTime));
            }
        });
    }

    @ParameterizedTest
    @MethodSource("markTasksForRefreshArgsProvider")
    public void should_test_procedure_call_mark_report_tasks_for_refresh(final String testCategory,
                                       final String testName,
                                       final Stream<String> taskParamsStream,
                                       final OffsetDateTime markBeforeTime,
                                       final Long expectedMarked) {
        List<TaskResource> tasks = new ArrayList<>();
        taskParamsStream.forEach(taskParamsString -> {
            String[] taskParams = taskParamsString.split(",");
            TaskResource taskResource = createAndSaveTask(
                taskParams[0],
                taskParams[1],
                taskParams[2],
                OffsetDateTime.parse(taskParams[3]),
                taskParams[4],
                taskParams[5]
            );
            log.info(taskResource.toString());
            tasks.add(taskResource);
        });

        tasks.forEach(task -> await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingServiceForTest.findByReportingTaskId(task.getTaskId());

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(task.getTaskId(), reportableTaskList.get(0).getTaskId());
                    assertEquals(task.getState().getValue(), reportableTaskList.get(0).getState());

                    return true;
                }));

        List<String> taskIds = tasks.stream().map(TaskResource::getTaskId).toList();
        List<String> caseIds = tasks.stream().map(TaskResource::getCaseId).toList();
        List<String> taskStates =   tasks.stream().map(x -> x.getState().getValue()).toList();

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                           containerReplica.getUsername(),
                                                           containerReplica.getPassword());

        List<Timestamp> taskRefreshTimestamps = miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(taskIds);
        taskRefreshTimestamps.forEach(Assertions::assertNull);

        switch (testCategory) {
            case "ProcTestsWithDefaultNulls" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(null, null, null,
                                              null, null, markBeforeTime);
            case "ProcTestsWithDefaultEmpty" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), "",
                                              "", Collections.emptyList(), markBeforeTime);
            case "MarkBeforeTimeTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                              null, Collections.emptyList(), markBeforeTime);
            case "TaskStateTests" -> {
                if ("matchPartialRecordsByMultipleStatuses".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                                  null, List.of("COMPLETED", "TERMINATED"), markBeforeTime);
                } else if ("matchNoRecordsByInvalidStatuses".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                                  null, List.of("DUMMY", "TEST"), markBeforeTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                                  null, taskStates, markBeforeTime);
                }
            }
            case "TaskIdTests" -> {
                if ("matchPartialRecordsByMultipleTaskIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), taskIds.subList(0, 2), null,
                                                  null, Collections.emptyList(), markBeforeTime);
                } else if ("matchNoRecordsByInvalidTaskIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(),
                                                  Collections.singletonList("alsdjf-aldsj-dummy"), null,
                                                  null, Collections.emptyList(), markBeforeTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), taskIds, null,
                                                  null, Collections.emptyList(), markBeforeTime);
                }
            }
            case "CaseIdTests" -> {
                if ("matchPartialRecordsByMultipleCaseIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds.subList(0, 2), Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime);
                } else if ("matchNoRecordsByInvalidCaseIds".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(Collections.singletonList("9999999"),
                                                  Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, Collections.emptyList(), null,
                                                  null, Collections.emptyList(), markBeforeTime);
                }
            }
            case "MarkByJurisdictionTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), "WA",
                                              "", Collections.emptyList(), markBeforeTime);
            case "MarkByCaseTypeIdTests" ->
                miReplicaDBDao.callMarkReportTasksForRefresh(Collections.emptyList(), Collections.emptyList(), null,
                                              "WAAPPS", Collections.emptyList(), markBeforeTime);
            case "MarkByAllParamsTests" -> {
                if ("matchPartialRecordsByAllParams".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "TEST",
                                                  "TESTAPPS", taskStates, markBeforeTime);
                } else if ("matchNoRecordsByInvalidParams".equals(testName)) {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                            "TESTAPPS", Collections.singletonList("DUMMY"), markBeforeTime);
                } else {
                    miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                                  "WAAPPS", taskStates, markBeforeTime);
                }
            }
            default -> miReplicaDBDao.callMarkReportTasksForRefresh(caseIds, taskIds, "WA",
                                                     "WAAPPS", taskStates, markBeforeTime);
        }
        taskRefreshTimestamps = miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(taskIds);
        Long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(expectedMarked, count, String.format("%s-%s:", testCategory, testName));
    }

    public Stream<Arguments> markTasksForRefreshArgsProvider() {
        String waCaseId = "1690202048809201";
        String terminatedStatus = "TERMINATED";
        String waCaseTypeId = "WAAPPS";
        String waJurisdiction = "WA";

        String testCaseId = "16902020444444";
        String unassignedStatus = "UNASSIGNED";
        String testCaseTypeId = "TESTAPPS";
        String testJurisdiction = "TEST";

        String assignedStatus = "ASSIGNED";
        String completedStatus = "COMPLETED";
        String testCaseId1 = "1690202055555";

        return Stream.of(
            Arguments.arguments(
                "ProcTestsWithDefaultNulls",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(25), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(24), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(23).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "ProcTestsWithDefaultEmpty",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                              OffsetDateTime.now().minusDays(27), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                              OffsetDateTime.now().minusDays(28), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(26).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            // The 4'th Argument markBeforeTime is used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "matchAllRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(20), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(17).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            // The 4'th Argument markBeforeTime is used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "matchPartialRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                              OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                              OffsetDateTime.now().minusDays(13), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(14).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L),

            // The 4'th Argument markBeforeTime is used as filtering criteria
            Arguments.arguments(
                "MarkBeforeTimeTests",
                "noneMatchedRecords",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(12), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), unassignedStatus,
                                  OffsetDateTime.now().minusDays(11), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(21).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "TaskStateTests",
                "matchAllRecordsBySingleStatus",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(15).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "TaskStateTests",
                "matchAllRecordsByMultipleStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "TaskStateTests",
                "matchPartialRecordsByMultipleStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "TaskStateTests",
                "matchNoRecordsByInvalidStatuses",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "TaskIdTests",
                "matchAllRecordsByMultipleTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "TaskIdTests",
                "matchPartialRecordsByMultipleTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "TaskIdTests",
                "matchNoRecordsByInvalidTaskIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "CaseIdTests",
                "matchAllRecordsByMultipleCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "CaseIdTests",
                "matchPartialRecordsByMultipleCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "CaseIdTests",
                "matchNoRecordsByInvalidCaseIds",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchNoRecordsByInvalidJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchOneRecordByJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L),

            Arguments.arguments(
                "MarkByJurisdictionTests",
                "matchMultipleRecordsByJurisdiction",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchNoRecordsByInvalidCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), testCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchOneRecordByCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                1L),

            Arguments.arguments(
                "MarkByCaseTypeIdTests",
                "matchMultipleRecordsByCaseTypeId",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(15), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(14), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchAllRecordsByAllParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, UUID.randomUUID(), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), waCaseTypeId, waJurisdiction)),
                OffsetDateTime.now().minusDays(15).withHour(10).withMinute(0).withSecond(0).withNano(0),
                4L),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchPartialRecordsByAllParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, UUID.randomUUID(), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                2L),

            Arguments.arguments(
                "MarkByAllParamsTests",
                "matchNoRecordsByInvalidParams",
                Stream.of(
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), completedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", waCaseId, UUID.randomUUID(), terminatedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId, UUID.randomUUID(), assignedStatus,
                                  OffsetDateTime.now().minusDays(18), waCaseTypeId, waJurisdiction),
                    String.format("%s,%s,%s,%s,%s,%s", testCaseId1, UUID.randomUUID(), unassignedStatus,
                                  OffsetDateTime.now().minusDays(17), testCaseTypeId, testJurisdiction)),
                OffsetDateTime.now().minusDays(13).withHour(10).withMinute(0).withSecond(0).withNano(0),
                0L)
        );
    }


    private void checkHistory(String id, int records) {
        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingServiceForTest.findByTaskId(id);

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals(records, taskHistoryResourceList.size());
                    log.info("task with ID found on MI Reporting task History is :{}",
                             taskHistoryResourceList.isEmpty());
                    return true;
                });
    }

    @ParameterizedTest
    @CsvSource(value = {
        "2, 10000, 2",
        "2, -20, 2",
        "2, 0, 2"
    })
    public void should_test_refresh_report_tasks(Integer taskResourcesToCreate,
                                                 Integer maxRowsToProcess,
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
        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                             containerReplica.getUsername(),
                                                             containerReplica.getPassword());

        List<Timestamp> taskRefreshTimestamps = miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(taskIds);
        taskRefreshTimestamps.forEach(Assertions::assertNull);

        miReplicaDBDao.callMarkReportTasksForRefresh(null, taskIds, null,
                                      null, null, OffsetDateTime.now());

        taskRefreshTimestamps = miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(taskIds);
        long count = taskRefreshTimestamps.stream().map(Objects::nonNull).count();
        Assertions.assertEquals(taskResourcesToCreate,
                                (int) count, String.format("Should mark all %s tasks:", taskResourcesToCreate));

        miReplicaDBDao.callRefreshReportTasks(maxRowsToProcess);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(5, SECONDS)
            .atMost(30, SECONDS)
            .until(
                () -> {
                    List<Timestamp> taskRefreshTimestampList =
                        miReplicaDBDao.callGetReplicaTaskRequestRefreshTimes(taskIds);
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

}
