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
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

/**
 * We test logical replication in here.
 */
@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MIReportingServiceTest extends SpringBootIntegrationBaseTest {

    @Autowired
    TaskResourceRepository taskResourceRepository;

    @Autowired
    DataSource dataSource;

    @Autowired
    TaskHistoryResourceRepository taskHistoryResourceRepository;
    @Autowired
    ReportableTaskRepository reportableTaskRepository;

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
