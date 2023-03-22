package uk.gov.hmcts.reform.wataskmanagementapi.services;

import junit.framework.AssertionFailedError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.entities.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNCONFIGURED;

/**
 * We test logical replication in here.
 */
@ActiveProfiles("integration")
class MIReportingServiceTest extends SpringBootIntegrationBaseTest {

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
        miReportingService = new MIReportingService(taskHistoryResourceRepository,
                                                    taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    "repl_user", "repl_password");
        cftTaskDatabaseService = new CFTTaskDatabaseService(taskResourceRepository);

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

    @Test
    void given_zero_publications_should_return_false() {
        TaskResourceRepository taskResourceRepository = mock(TaskResourceRepository.class);
        when(taskResourceRepository.countPublications()).thenReturn(0);
        miReportingService = new MIReportingService(null, taskResourceRepository,
                                                    reportableTaskRepository,
                                                    taskAssignmentsRepository,
                                                    "repl_user", "repl_password");

        assertFalse(miReportingService.isPublicationPresent());
    }

    private TaskResource createAndSaveTask() {
        TaskResource taskResource = new TaskResource(
            UUID.randomUUID().toString(),
            "someTaskName",
            "someTaskType",
            UNCONFIGURED,
            "987654",
            OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00")
        );
        taskResource.setCreated(OffsetDateTime.now());
        taskResource.setPriorityDate(OffsetDateTime.parse("2022-05-09T20:15:45.345875+01:00"));
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
            "Assign");

        return taskResourceRepository.save(taskResource);
    }
}
