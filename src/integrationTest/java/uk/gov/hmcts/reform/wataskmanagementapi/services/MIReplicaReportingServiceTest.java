package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

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
                        assertEquals(null, reportableTaskList.get(0).getIsWithinSla());
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
            TEST_PRIMARY_DB_USER, TEST_PRIMARY_DB_PASS);
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
    void should_report_incomplete_task_history(String initialState, String lastAction) throws Exception {
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
            OffsetDateTime.now().plusDays(daysUntilDue)
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.now().plusDays(daysUntilPriority));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.now());
        taskResource.setLastUpdatedAction("Configure");
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
}
