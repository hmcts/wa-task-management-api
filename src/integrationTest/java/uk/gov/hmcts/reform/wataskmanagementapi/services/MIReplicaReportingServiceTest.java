package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

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
class MIReplicaReportingServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    @Autowired
    DataSource dataSource;

    @Autowired
    TaskHistoryResourceRepository taskHistoryResourceRepository;
    @Autowired
    ReportableTaskRepository reportableTaskRepository;
    @Autowired
    TaskAssignmentsRepository taskAssignmentsRepository;

    SubscriptionCreator subscriptionCreator;

    @Autowired
    TCExtendedContainerDatabaseDriver tcDriver;

    @Value("${spring.datasource.jdbcUrl}")
    private String primaryJdbcUrl;


    @Value("${spring.datasource-replica.jdbcUrl}")
    private String replicaJdbcUrl;

    CFTTaskDatabaseService cftTaskDatabaseService;
    MIReportingService miReportingService;

    JdbcDatabaseContainer container;
    JdbcDatabaseContainer containerReplica;

    @BeforeEach
    void setUp() {
        subscriptionCreator = new SubscriptionCreator("repl_user", "repl_password",
                                                      "wa_user", "wa_password");
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    subscriptionCreator);
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);

        container = TCExtendedContainerDatabaseDriver.getContainer(primaryJdbcUrl);
        containerReplica = TCExtendedContainerDatabaseDriver.getContainer(replicaJdbcUrl);
        Testcontainers.exposeHostPorts(container.getFirstMappedPort(), containerReplica.getFirstMappedPort());
    }

    @AfterAll
    void tearDown() {
        container.stop();
        containerReplica.stop();
    }

    @Test
    void should_save_task_and_get_task_from_replica_tables() {
        TaskResource taskResource = createAndSaveTask();

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = miReportingService.findByTaskId(taskResource.getTaskId());

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
                        = miReportingService.findByReportingTaskId(taskResource.getTaskId());

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
                        = miReportingService.findByReportingTaskId(taskResource.getTaskId());

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
                        = miReportingService.findByReportingTaskId(taskId);

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
                        = miReportingService.findByAssignmentsTaskId(taskResource.getTaskId());

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
        TaskResource taskResource = createAndAssignTask();
        String taskId = taskResource.getTaskId();
        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingService.findByAssignmentsTaskId(taskId);

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

        checkHistory(taskId, 2);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingService.findByAssignmentsTaskId(taskResource.getTaskId());

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
                        = miReportingService.findByReportingTaskId(taskId);

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
                        = miReportingService.findByTaskId(taskId);
                    assertEquals(2, taskHistoryList.size());
                    return true;
                });

        if (lastAction.equals("Complete")) {
            await().ignoreException(AssertionFailedError.class)
                .pollInterval(1, SECONDS)
                .atMost(10, SECONDS)
                .until(
                    () -> {
                        List<ReportableTaskResource> reportableTaskList
                            = miReportingService.findByReportingTaskId(taskId);

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
                        = miReportingService.findByAssignmentsTaskId(taskResource.getTaskId());

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
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        subscriptionCreator = new SubscriptionCreator("repl_user", "repl_password",
                                                      "repl_user", "repl_password");
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    subscriptionCreator);

        assertFalse(miReportingService.isPublicationPresent());
    }

    @Test
    void given_unknown_task_id_what_happens() {
        List<TaskHistoryResource> taskHistoryResourceList
            = miReportingService.findByTaskId("1111111");
        assertTrue(taskHistoryResourceList.isEmpty());
    }


    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,Configure",
        "UNASSIGNED,Claim",
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
                        = miReportingService.findByReportingTaskId(taskId);

                    if (initialState.equals("UNASSIGNED") && lastAction.equals("Configure")) {
                        assertFalse(reportableTaskList.isEmpty());
                        assertFalse(containerReplica.getLogs().contains(taskId + " : Task with an incomplete history"));
                    } else {
                        assertTrue(reportableTaskList.isEmpty());
                        assertTrue(containerReplica.getLogs().contains(taskId + " : Task with an incomplete history"));
                    }
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
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-15T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction("Configure");
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
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.parse("2022-05-05T20:15:45.345875+01:00"));
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2022-05-07T20:15:45.345875+01:00"));
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
                        = miReportingService.findByReportingTaskId(taskId);

                    assertFalse(reportableTaskList.isEmpty());
                    assertEquals(1, reportableTaskList.size());
                    assertEquals(taskId, reportableTaskList.get(0).getTaskId());
                    assertEquals("SecondTask", reportableTaskList.get(0).getTaskName());

                    List<TaskHistoryResource> taskHistoryList
                        = miReportingService.findByTaskId(taskId);
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
                        = miReportingService.findByTaskId(id);

                    assertFalse(taskHistoryResourceList.isEmpty());
                    assertEquals(records, taskHistoryResourceList.size());

                    return true;
                });
    }
}
