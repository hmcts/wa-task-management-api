package uk.gov.hmcts.reform.wataskmanagementapi.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.JdbcDatabaseContainer;
import uk.gov.hmcts.reform.wataskmanagementapi.SpringBootIntegrationBaseTest;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.SubscriptionCreator;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.db.TCExtendedContainerDatabaseDriver;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ReplicationChecker;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@ActiveProfiles("replica")
public class ReplicationCheckerTest extends SpringBootIntegrationBaseTest {

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

    @Autowired
    ReplicationChecker replicationChecker;

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

        TaskOperationRequest request = new TaskOperationRequest(
            TaskOperation.builder()
                .type(TaskOperationType.PERFORM_REPLICATION_CHECK).build(),
            List.of()
        );

        Map<String, Object> resourceMap = replicationChecker.performOperation(
            request
        ).getResponseMap();

        List<?> tasks = (List<?>) resourceMap.get("replicationCheckedTaskIds");
        assertEquals(1, tasks.size());
        List<?> notReplicated = (List<?>) resourceMap.get("notReplicatedTaskIds");
        assertEquals(0, notReplicated.size());

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
}
