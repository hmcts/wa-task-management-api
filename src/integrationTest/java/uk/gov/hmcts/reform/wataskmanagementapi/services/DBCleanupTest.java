package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.opentest4j.AssertionFailedError;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.wataskmanagementapi.db.MIReplicaDBDao;
import uk.gov.hmcts.reform.wataskmanagementapi.db.PrimaryDBDao;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.ReportableTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.SensitiveTaskEventLog;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskAssignmentsResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskHistoryResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.TaskRoleResource;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.Users;
import uk.gov.hmcts.reform.wataskmanagementapi.entity.replica.ReplicaTaskResource;
import uk.gov.hmcts.reform.wataskmanagementapi.exceptions.ReplicationException;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.SensitiveTaskEventLogsRepository;
import uk.gov.hmcts.reform.wataskmanagementapi.repository.TaskRoleResourceRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.singleton;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static uk.gov.hmcts.reform.wataskmanagementapi.cft.enums.CFTTaskState.ASSIGNED;

@ActiveProfiles("replica")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
public class DBCleanupTest extends ReplicaBaseTest {

    @Mock
    PrimaryDBDao primaryDBDaoForException;

    @Autowired
    TaskRoleResourceRepository taskRoleResourceRepository;

    @Autowired
    SensitiveTaskEventLogsRepository sensitiveTaskEventLogsRepository;

    @BeforeAll
    public void setup() {
        PrimaryDBDao primaryDBDao = new PrimaryDBDao(container.getJdbcUrl(),
                                                     container.getUsername(),
                                                     container.getPassword());

        String primaryCleanupFunction =
            readSqlFileToString("src/integrationTest/resources/cleanup/non_prod_primary_cleanup.sql");
        primaryDBDao.insertPrimaryCleanupFunction(primaryCleanupFunction);


        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                           containerReplica.getUsername(),
                                                           containerReplica.getPassword());

        String replicaCleanupFunction =
            readSqlFileToString("src/integrationTest/resources/cleanup/non_prod_replica_cleanup.sql");
        miReplicaDBDao.inserReplicaCleanUpFunction(replicaCleanupFunction);
    }

    @Test
    public void when_cleanup_scripts_invoked_should_clear_old_tasks_from_primary_and_replica() {

        List<String> taskIdList = createTasks(5);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourceList
                        = taskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                   Sort.by(Sort.Order.asc("created")));

                    assertFalse(taskResourceList.isEmpty());
                    assertEquals(5, taskResourceList.size());

                    List<TaskRoleResource> taskRoleResourceList
                        = taskRoleResourceRepository.findAllByTaskIdIn(taskIdList);

                    assertFalse(taskRoleResourceList.isEmpty());
                    assertEquals(5, taskRoleResourceList.size());

                    List<SensitiveTaskEventLog> sensitiveTaskEventLogList
                        = sensitiveTaskEventLogsRepository.findAllByTaskIdIn(taskIdList);

                    assertFalse(sensitiveTaskEventLogList.isEmpty());
                    assertEquals(5, sensitiveTaskEventLogList.size());

                    List<ReplicaTaskResource> replicaTaskResourceList
                        = replicaTaskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                          Sort.by(Sort.Order.asc("created")));

                    assertFalse(replicaTaskResourceList.isEmpty());
                    assertEquals(5, replicaTaskResourceList.size());
                    return true;
                });

        PrimaryDBDao primaryDBDao = new PrimaryDBDao(container.getJdbcUrl(),
                                                     container.getUsername(),
                                                     container.getPassword());

        primaryDBDao.callPrimaryCleanupFunction(OffsetDateTime.parse("2023-11-22T20:10:45.345875+01:00"),
                                                OffsetDateTime.parse("2023-11-24T20:45:45.345875+01:00"));

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourceList
                        = taskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                   Sort.by(Sort.Order.asc("created")));

                    assertTrue(taskResourceList.isEmpty());

                    List<TaskRoleResource> taskRoleResourceList
                        = taskRoleResourceRepository.findAllByTaskIdIn(taskIdList);

                    assertTrue(taskRoleResourceList.isEmpty());

                    List<SensitiveTaskEventLog> sensitiveTaskEventLogList
                        = sensitiveTaskEventLogsRepository.findAllByTaskIdIn(taskIdList);

                    assertTrue(sensitiveTaskEventLogList.isEmpty());

                    List<ReplicaTaskResource> replicaTaskResourceList
                        = replicaTaskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                          Sort.by(Sort.Order.asc("created")));

                    assertTrue(replicaTaskResourceList.isEmpty());
                    return true;
                });

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                           containerReplica.getUsername(),
                                                           containerReplica.getPassword());

        miReplicaDBDao.callReplicaCleanupFunction(OffsetDateTime.parse("2023-11-22T20:10:45.345875+01:00"),
                                                  OffsetDateTime.parse("2023-11-24T20:45:45.345875+01:00"));

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {

                    List<ReportableTaskResource> reportableTaskResourceList
                        = reportableTaskRepository.findAllByTaskIdIn(taskIdList);

                    assertTrue(reportableTaskResourceList.isEmpty());

                    List<TaskAssignmentsResource> taskAssignmentsResourceList
                        = taskAssignmentsRepository.findAllByTaskIdIn(taskIdList);

                    assertTrue(taskAssignmentsResourceList.isEmpty());

                    List<TaskHistoryResource> taskHistoryResourceList
                        = taskHistoryResourceRepository.findAllByTaskIdIn(taskIdList);

                    assertTrue(taskHistoryResourceList.isEmpty());

                    return true;
                });

    }

    @Test
    public void when_cleanup_scripts_throw_exception_should_not_clear_old_tasks_from_primary_and_replica() {


        List<String> taskIdList = createTasks(5);

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourceList
                        = taskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                   Sort.by(Sort.Order.asc("created")));

                    assertFalse(taskResourceList.isEmpty());
                    assertEquals(5, taskResourceList.size());

                    List<TaskRoleResource> taskRoleResourceList
                        = taskRoleResourceRepository.findAllByTaskIdIn(taskIdList);

                    assertFalse(taskRoleResourceList.isEmpty());
                    assertEquals(5, taskRoleResourceList.size());

                    List<SensitiveTaskEventLog> sensitiveTaskEventLogList
                        = sensitiveTaskEventLogsRepository.findAllByTaskIdIn(taskIdList);

                    assertFalse(sensitiveTaskEventLogList.isEmpty());
                    assertEquals(5, sensitiveTaskEventLogList.size());

                    List<ReplicaTaskResource> replicaTaskResourceList
                        = replicaTaskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                          Sort.by(Sort.Order.asc("created")));

                    assertFalse(replicaTaskResourceList.isEmpty());
                    assertEquals(5, replicaTaskResourceList.size());
                    return true;
                });

        doThrow(new ReplicationException("Simulated DB error",new Exception()))
            .when(primaryDBDaoForException).callPrimaryCleanupFunction(any(OffsetDateTime.class),
                                                                       any(OffsetDateTime.class));

        Assertions.assertThatThrownBy(() -> primaryDBDaoForException.callPrimaryCleanupFunction(OffsetDateTime.now(),
                                                                                                OffsetDateTime.now())
            ).isInstanceOf(ReplicationException.class)
            .hasMessageContaining("Simulated DB error");

        await().ignoreException(AssertionFailedError.class)
            .pollInterval(1, SECONDS)
            .atMost(10, SECONDS)
            .until(
                () -> {
                    List<TaskResource> taskResourceList
                        = taskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                   Sort.by(Sort.Order.asc("created")));

                    assertFalse(taskResourceList.isEmpty());
                    assertEquals(5, taskResourceList.size());

                    List<ReplicaTaskResource> replicaTaskResourceList
                        = replicaTaskResourceRepository.findAllByTaskIdIn(taskIdList,
                                                                          Sort.by(Sort.Order.asc("created")));

                    assertFalse(replicaTaskResourceList.isEmpty());
                    assertEquals(5, replicaTaskResourceList.size());
                    return true;
                });

    }


    private List<String> createTasks(int numberOfTasks) {
        List<String> taskIds = new ArrayList<>();
        OffsetDateTime createdDate = OffsetDateTime.parse("2023-11-23T20:15:45.345875+01:00");

        for (int i = 0; i < numberOfTasks; i++) {
            TaskResource taskResource = new TaskResource(
                UUID.randomUUID().toString(),
                "someCaseId",
                "IA",
                "someLocation",
                "LEGAL",
                "someTaskName"
            );
            taskResource.setTaskRoleResources(singleton(createTaskRoleResource(taskResource.getTaskId())));
            taskResource.setDueDateTime(OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00"));
            taskResource.setLastUpdatedTimestamp(OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00"));
            taskResource.setPriorityDate(OffsetDateTime.parse("2023-03-26T20:15:45.345875+01:00"));
            taskResource.setCreated(createdDate.plusMinutes(i * 5L));
            taskResource.setAssignee("someAssignee");
            taskResource.setLastUpdatedAction("AutoAssign");
            taskResource.setState(ASSIGNED);
            taskResourceRepository.save(taskResource);
            taskIds.add(taskResource.getTaskId());
            sensitiveTaskEventLogsRepository.save(createSensitiveTaskEventLog(taskResource));
        }
        return taskIds;
    }

    private TaskRoleResource createTaskRoleResource(String taskId) {
        return new TaskRoleResource(
            "tribunal-caseworker",
            true,
            true,
            false,
            true,
            true,
            true,
            new String[]{"IA"},
            0,
            true,
            "any-category",
            taskId,
            OffsetDateTime.now()
        );
    }

    private SensitiveTaskEventLog createSensitiveTaskEventLog(TaskResource taskResource) {
        return new SensitiveTaskEventLog(
            UUID.randomUUID().toString(),
            "someCorrelationId",
            taskResource.getTaskId(),
            "someCaseId",
            "someMessage",
            Collections.singletonList(taskResource),
            new Users(),
            OffsetDateTime.parse("2023-04-05T20:15:45.345875+01:00"),
            OffsetDateTime.parse("2023-03-29T20:15:45.345875+01:00")
        );
    }

    public static String readSqlFileToString(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
