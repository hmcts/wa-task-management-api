package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.ReportableTaskRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskAssignmentsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.cft.replicarepository.TaskHistoryResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.config.IntegrationSecurityTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.TaskOperationRequest;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.entities.TaskOperation;
import uk.gov.hmcts.reform.wataskmanagementapi.controllers.request.enums.TaskOperationType;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskResourceRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.services.operation.ReplicationChecker;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.AwaitilityIntegrationTestConfig;
import uk.gov.hmcts.reform.wataskmanagementapi.utils.ReplicaIntegrationTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.UNASSIGNED;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestInstance(PER_CLASS)
@Slf4j
@ActiveProfiles({"replica"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Import({AwaitilityIntegrationTestConfig.class, IntegrationSecurityTestConfig.class})
public class ReplicationCheckerTest {

    @Autowired
    ReplicationChecker replicationChecker;

    @Autowired
    private TaskResourceRepository taskResourceRepository;
    @Autowired
    private TaskHistoryResourceRepository taskHistoryResourceRepository;
    @Autowired
    private ReportableTaskRepository reportableTaskRepository;
    @Autowired
    private TaskAssignmentsRepository taskAssignmentsRepository;
    @Autowired
    private MIReportingService miReportingService;
    @Value("${spring.datasource.jdbcUrl}")
    private String primaryJdbcUrl;
    @Value("${spring.datasource-replica.jdbcUrl}")
    private String replicaJdbcUrl;

    ReplicaIntegrationTestUtils replicaIntegrationTestUtils;

    @BeforeAll
    void init() {
        replicaIntegrationTestUtils = new ReplicaIntegrationTestUtils(
            taskResourceRepository,
            taskHistoryResourceRepository,
            reportableTaskRepository,
            taskAssignmentsRepository,
            miReportingService,
            primaryJdbcUrl,
            replicaJdbcUrl
        );
        replicaIntegrationTestUtils.setUp();
    }

    @AfterAll
    void tearDown() {
        replicaIntegrationTestUtils.tearDown();
    }

    @Test
    void should_save_task_and_get_task_from_replica_tables() {
        TaskResource taskResource = createAndSaveTask();

        await()
            .until(
                () -> {
                    List<TaskHistoryResource> taskHistoryResourceList
                        = replicaIntegrationTestUtils.getMiReportingServiceForTest()
                        .findByTaskId(taskResource.getTaskId());

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
