package uk.gov.hmcts.reform.wataskmanagementapi.services;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    public static final String PRIMARY_CLEANUP_FUNCTION =
        "CREATE OR REPLACE FUNCTION "
            + "cft_task_db.task_cleanup_between_dates_primary(created_time_from TIMESTAMP,created_time_to TIMESTAMP)\n"
            + "RETURNS VOID\n"
            + "LANGUAGE plpgsql\n"
            + "AS $function$\n"
            + "DECLARE\n"
            + "    task_id_var TEXT;\n"
            + "BEGIN\n"
            + "    -- Loop through the rows of the tasks table\n"
            + "    FOR task_id_var IN "
            + "SELECT task_id FROM cft_task_db.tasks WHERE created >= created_time_from "
            + "AND created <= created_time_to order by created limit 2000 LOOP\n"
            + "\n"
            + "        -- delete from sensitive_task_event_logs\n"
            + "        DELETE FROM cft_task_db.sensitive_task_event_logs WHERE task_id = task_id_var;\n"
            + "\n"
            + "      IF EXISTS (SELECT 1 FROM cft_task_db.sensitive_task_event_logs WHERE task_id = task_id_var) THEN\n"
            + "            RAISE NOTICE 'Issue with the taskId: % in sensitive_task_event_logs table. "
            + "Skipping next steps', task_id_var;\n"
            + "            CONTINUE;\n"
            + "        END IF;\n"
            + "\n"
            + "        -- delete from task_roles\n"
            + "        DELETE FROM cft_task_db.task_roles WHERE task_id = task_id_var;\n"
            + "\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.task_roles WHERE task_id = task_id_var) THEN\n"
            + "       RAISE NOTICE 'Issue with the taskId: % in task_roles table. Skipping next steps', task_id_var;\n"
            + "            CONTINUE;\n"
            + "        END IF;\n"
            + "\n"
            + "        -- delete from tasks\n"
            + "        DELETE FROM cft_task_db.tasks WHERE task_id = task_id_var;\n"
            + "\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = task_id_var) THEN\n"
            + "            RAISE NOTICE 'Issue with the taskId: % in tasks table', task_id_var;\n"
            + "        END IF;\n"
            + "\n"
            + "    END LOOP;\n"
            + "\n"
            + "    RETURN;\n"
            + "\n"
            + "END;\n"
            + "$function$";

    public static final String REPLICA_CLEANUP_FUNCTION =
        "CREATE OR REPLACE FUNCTION "
            + "cft_task_db.task_cleanup_between_dates_replica(created_time_from TIMESTAMP,created_time_to TIMESTAMP)\n"
            + "RETURNS VOID\n"
            + "LANGUAGE plpgsql\n"
            + "AS $function$\n"
            + "DECLARE\n"
            + "    task_id_var TEXT;\n"
            + "    created_date timestamp;\n"
            + "BEGIN\n"
            + "\n"
            + "    -- Loop through the rows of the tasks table\n"
            + "    FOR task_id_var,created_date IN "
            + "SELECT DISTINCT task_id,created FROM cft_task_db.task_history WHERE created >= created_time_from "
            + "AND created <= created_time_to order by created limit 10000 LOOP\n"
            + "\n"
            + "        -- Check if any more rows exist in tasks\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.tasks WHERE task_id = task_id_var) THEN\n"
            + "            RAISE NOTICE 'Issue with the taskId: % in tasks table. Skipping next steps', task_id_var;\n"
            + "            CONTINUE;\n"
            + "        END IF;\n"
            + "\n"
            + "        -- delete from reportable_task\n"
            + "        DELETE FROM cft_task_db.reportable_task WHERE task_id = task_id_var;\n"
            + "\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.reportable_task WHERE task_id = task_id_var) THEN\n"
            + "            RAISE NOTICE 'Issue with the taskId: % in reportable_task table', task_id_var;\n"
            + "            CONTINUE;\n"
            + "        END IF;\n"
            + "\n"
            + "        -- delete from task_assignments\n"
            + "        DELETE FROM cft_task_db.task_assignments WHERE task_id = task_id_var;\n"
            + "\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.task_assignments WHERE task_id = task_id_var) THEN\n"
            + "          RAISE NOTICE 'Issue with the taskId: % in task_assignments table', task_id_var;\n"
            + "          CONTINUE;\n"
            + "        END IF;\n"
            + "\n"
            + "        -- delete from task_history\n"
            + "        DELETE FROM cft_task_db.task_history WHERE task_id = task_id_var;\n"
            + "\n"
            + "        IF EXISTS (SELECT 1 FROM cft_task_db.task_history WHERE task_id = task_id_var) THEN\n"
            + "            RAISE NOTICE 'Issue with the taskId: % in task_history table', task_id_var;\n"
            + "        END IF;\n"
            + "\n"
            + "    END LOOP;\n"
            + "\n"
            + "    RETURN;\n"
            + "\n"
            + "END;\n"
            + "$function$";


    @BeforeEach
    public void setup() {
        PrimaryDBDao primaryDBDao = new PrimaryDBDao(container.getJdbcUrl(),
                                                     container.getUsername(),
                                                     container.getPassword());

        primaryDBDao.insertPrimaryCleanupFunction(PRIMARY_CLEANUP_FUNCTION);

        MIReplicaDBDao miReplicaDBDao = new MIReplicaDBDao(containerReplica.getJdbcUrl(),
                                                           containerReplica.getUsername(),
                                                           containerReplica.getPassword());

        miReplicaDBDao.inserReplicaCleanUpFunction(REPLICA_CLEANUP_FUNCTION);
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
}
