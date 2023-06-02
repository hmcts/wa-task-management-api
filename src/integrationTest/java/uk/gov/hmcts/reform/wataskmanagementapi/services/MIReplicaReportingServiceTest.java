package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @BeforeEach
    void setUp() {
        subscriptionCreator = new SubscriptionCreator("repl_user", "repl_password",
                                                      "repl_user", "repl_password");
        miReportingService = new MIReportingService(taskHistoryResourceRepository, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    subscriptionCreator);
        CFTTaskMapper cftTaskMapper = new CFTTaskMapper(new ObjectMapper());
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository, cftTaskMapper);

        JdbcDatabaseContainer container = TCExtendedContainerDatabaseDriver.getContainer(primaryJdbcUrl);
        JdbcDatabaseContainer containerReplica = TCExtendedContainerDatabaseDriver.getContainer(replicaJdbcUrl);
        Testcontainers.exposeHostPorts(container.getFirstMappedPort(), containerReplica.getFirstMappedPort());
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
                    assertEquals(taskResource.getLastUpdatedTimestamp(), taskHistoryResourceList.get(0).getUpdated());
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
                    assertEquals(taskResource.getLastUpdatedTimestamp(), reportableTaskList.get(0).getUpdated());
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    return true;
                });
    }

    @Test
    void should_save_AutoAssign_task_and_get_task_from_reportable_task() {
        String taskId = UUID.randomUUID().toString();
        TaskResource taskResource = createAndSaveThisTask(taskId, "FirstTask", ASSIGNED, "AutoAssign");

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
                    assertEquals(taskResource.getLastUpdatedTimestamp(), reportableTaskList.get(0).getUpdated());
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    return true;
                });
    }

    @Test
    void should_save_task_and_get_task_from_task_assignments() {
        TaskResource taskResource = createAndAssignTask();

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
                    assertEquals(taskResource.getState().toString(), reportableTaskList.get(0).getState());
                    assertEquals(taskResource.getLastUpdatedUser(), reportableTaskList.get(0).getUpdatedBy());
                    assertEquals(taskResource.getLastUpdatedAction(), reportableTaskList.get(0).getUpdateAction());

                    return true;
                });

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
        "COMPLETED,Complete,COMPLETED",
        "ASSIGNED,AutoUnassignAssign,REASSIGNED",
        "ASSIGNED,UnassignAssign,REASSIGNED",
        "ASSIGNED,UnassignClaim,REASSIGNED",
        "UNASSIGNED,UnclaimAssign,REASSIGNED",
        "UNASSIGNED,Unassign,UNASSIGNED",
        "UNASSIGNED,AutoUnassign,UNASSIGNED",
        "UNASSIGNED,Unclaim,UNCLAIMED",
        "ASSIGNED,AutoCancel,CANCELLED",
        "ASSIGNED,Cancel,CANCELLED",
    })
    void should_save_task_and_check_task_assignments(String newState, String lastAction, String endReason) {
        TaskResource taskResource = createAndAssignTask();

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

        if (lastAction.matches("Unassign|Unclaim|AutoUnassign")) {
            taskResource.setAssignee(null);
        }
        if (lastAction.matches("AutoUnassignAssign|UnassignAssign|UnassignClaim|UnclaimAssign|Assign")) {
            taskResource.setAssignee("newAssignee");
        }
        taskResource.setLastUpdatedAction(lastAction);
        taskResource.setState(CFTTaskState.valueOf(newState));
        taskResourceRepository.save(taskResource);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskAssignmentsResource> taskAssignmentsList
                        = miReportingService.findByAssignmentsTaskId(taskResource.getTaskId());

                    System.out.println("taskAssignmentsList.get(0): " + taskAssignmentsList.get(0));

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

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNASSIGNED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
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
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
        taskResource.setLastUpdatedAction(lastAction);
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndAssignTask() {

        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someNewCaseId",
            "someJurisdiction",
            "someLocation",
            "someRoleCategory",
            "someTaskName",
            OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2023-03-23T20:15:45.345875+01:00"),
            "someAssignee",
            "AutoAssign");

        taskResource.setState(ASSIGNED);
        return taskResourceRepository.save(taskResource);
    }

    private TaskResource createAndSaveTaskWithLastReconfigurationTime(String taskId, String taskName,
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
        return taskResourceRepository.save(taskResource);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign",
        "ASSIGNED,Configure"
    })
    void should_insert_first_task_and_update_reconfiguration_task(String state, String lastAction) {
        String taskId = UUID.randomUUID().toString();
        createAndSaveThisTask(taskId, "FirstTask", CFTTaskState.valueOf(state), lastAction);
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

    @ParameterizedTest
    @CsvSource(value = {
        "UNASSIGNED,Configure",
        "ASSIGNED,AutoAssign"
    })
    void should_ignore_insert_reconfiguration_task(String state, String lastAction) {
        String taskId = UUID.randomUUID().toString();
        createAndSaveTaskWithLastReconfigurationTime(taskId,
                                                     "SecondTask", CFTTaskState.valueOf(state), lastAction);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<ReportableTaskResource> reportableTaskList
                        = miReportingService.findByReportingTaskId(taskId);

                    assertTrue(reportableTaskList.isEmpty());
                    List<TaskHistoryResource> taskHistoryList
                        = miReportingService.findByTaskId(taskId);
                    assertEquals(1, taskHistoryList.size());
                    return true;
                });
    }

}
